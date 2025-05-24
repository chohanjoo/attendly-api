package com.attendly.config

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.core.AppenderBase
import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Discord 웹훅으로 로그를 전송하는 Logback 어펜더
 */
@Component
class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {
    private var client: WebhookClient? = null
    private var applicationName: String = "Attendly API"
    private var environment: String = "UNKNOWN"
    private var minLevel: String = "WARN"
    private var maxMessageLength: Int = 1500
    private var enableFileAttachment: Boolean = true
    private var waitBetweenMessages: Long = 1500 // 메시지 전송 간 기본 대기 시간(ms) 증가
    private var batchSize: Int = 3 // 일괄 처리할 메시지 수 (감소)
    private var retryCount: Int = 5 // 재시도 횟수 (증가)
    
    // 속도 제한 관련 설정
    private var baseBackoffTime: Long = 1000 // 기본 백오프 시간 (ms)
    private var maxBackoffTime: Long = 30000 // 최대 백오프 시간 (ms)
    private var currentBackoffTime: Long = baseBackoffTime
    private var lastRateLimitTime: Long = 0
    private var consecutiveRateLimits: Int = 0
    
    // 메시지 큐 및 스케줄러
    private val messageQueue = ConcurrentLinkedQueue<LogMessage>()
    private val isProcessing = AtomicBoolean(false)
    private var executor: ScheduledThreadPoolExecutor? = null
    
    // Swagger 로그 수집용 버퍼
    private val swaggerLogsBuffer = StringBuilder()
    private var lastSwaggerLogTimestamp = 0L
    private val SWAGGER_LOG_TIMEOUT = 5000L // 5초 내에 연속된 스웨거 로그는 하나로 묶음
    
    // 로그 메시지 클래스
    private sealed class LogMessage {
        data class EmbedMessage(val embed: WebhookEmbed) : LogMessage()
        data class FileMessage(
            val embed: WebhookEmbed,
            val fileName: String, 
            val content: ByteArray
        ) : LogMessage() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as FileMessage

                if (embed != other.embed) return false
                if (fileName != other.fileName) return false
                if (!content.contentEquals(other.content)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = embed.hashCode()
                result = 31 * result + fileName.hashCode()
                result = 31 * result + content.contentHashCode()
                return result
            }
        }
    }
    
    /**
     * 메시지 내용을 포맷팅합니다. SQL 쿼리가 포함된 경우 코드 블록으로 감싸 인덱스가 깨지지 않도록 합니다.
     */
    private fun formatMessageContent(message: String): String {
        // SQL 쿼리 패턴 확인 (select, insert, update, delete 등으로 시작하는 문장)
        val sqlPatterns = listOf(
            "select\\s+.*from\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "insert\\s+into\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "update\\s+.*set\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "delete\\s+from\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*from\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*where\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*join\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*group\\s+by\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*order\\s+by\\s+.*".toRegex(RegexOption.IGNORE_CASE),
            "^\\s*having\\s+.*".toRegex(RegexOption.IGNORE_CASE)
        )

        // JSON 패턴 확인
        val jsonPattern = "^\\s*\\{.*\\}\\s*$".toRegex(RegexOption.DOT_MATCHES_ALL)
        
        return when {
            // SQL 쿼리인 경우 SQL 코드 블록으로 감싸기
            sqlPatterns.any { it.containsMatchIn(message) } -> {
                "```sql\n$message\n```"
            }
            // JSON인 경우 JSON 코드 블록으로 감싸기
            jsonPattern.matches(message) -> {
                "```json\n$message\n```"
            }
            // 일반 텍스트인 경우 그대로 반환
            else -> message
        }
    }
    
    /**
     * 로그 레벨에 따른 색상을 반환합니다
     */
    private fun getColorForLevel(level: String): Int {
        return when (level) {
            "ERROR" -> Color.RED.rgb
            "WARN" -> Color.ORANGE.rgb
            "INFO" -> Color.GREEN.rgb
            "DEBUG" -> Color.BLUE.rgb
            "TRACE" -> Color.LIGHT_GRAY.rgb
            else -> Color.GRAY.rgb
        }
    }

    /**
     * 현재 로그 이벤트가 설정된 최소 로그 레벨 이상인지 확인합니다
     */
    private fun isLevelEligible(event: ILoggingEvent): Boolean {
        val eventLevel = event.level.toString()
        val levelOrder = mapOf(
            "ERROR" to 0,
            "WARN" to 1,
            "INFO" to 2,
            "DEBUG" to 3,
            "TRACE" to 4
        )
        
        val eventLevelValue = levelOrder[eventLevel] ?: 4
        val minLevelValue = levelOrder[minLevel] ?: 1
        
        return eventLevelValue <= minLevelValue
    }
    
    /**
     * 어펜더가 시작될 때 호출되는 메서드
     * 웹훅 클라이언트를 초기화합니다
     */
    override fun start() {
        val webhookUrl = getDiscordWebhookUrl()
        
        if (webhookUrl.isNullOrBlank()) {
            addWarn("No webhookUrl set for DiscordWebhookAppender. Discord logging will be disabled.")
            // 웹훅 URL이 없더라도 어펜더는 시작하여 다른 로깅에 영향을 주지 않음
            super.start()
            return
        }
        
        try {
            // 전송 속도 제한 및 429 오류 처리를 위한 설정
            val clientBuilder = WebhookClientBuilder(webhookUrl)
            clientBuilder.setWait(true) // 응답 대기 설정
            client = clientBuilder.build()
            
            // 메시지 처리용 스케줄러 초기화 (스레드 풀 크기 축소)
            executor = ScheduledThreadPoolExecutor(1)
            executor?.scheduleAtFixedRate(
                { processMessageQueue() },
                waitBetweenMessages,
                waitBetweenMessages,
                TimeUnit.MILLISECONDS
            )
            
            // Swagger 로그 처리용 스케줄러 추가
            executor?.scheduleAtFixedRate(
                { processSwaggerLogs() },
                SWAGGER_LOG_TIMEOUT,
                SWAGGER_LOG_TIMEOUT,
                TimeUnit.MILLISECONDS
            )
            
            super.start()
        } catch (e: Exception) {
            addError("Failed to initialize Discord webhook client", e)
            // 예외가 발생해도 어펜더는 시작
            super.start()
        }
    }

    /**
     * Vault에서 Discord Webhook URL을 가져옵니다.
     */
    private fun getDiscordWebhookUrl(): String? {
        try {
            // ApplicationContextProvider를 통해 VaultDiscordConfig 빈 가져오기
            val vaultDiscordConfig = ApplicationContextProvider.getBean(VaultDiscordConfig::class.java)
            return vaultDiscordConfig?.webhook?.url
        } catch (e: Exception) {
            addError("Failed to get Discord webhook URL from Vault", e)
            return null
        }
    }

    /**
     * 어펜더가 종료될 때 호출되는 메서드
     * 웹훅 클라이언트 리소스를 해제합니다
     */
    override fun stop() {
        executor?.shutdown()
        try {
            // 종료 시 남은 메시지 전송 시도
            if (executor?.awaitTermination(30, TimeUnit.SECONDS) == false) {
                executor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor?.shutdownNow()
            Thread.currentThread().interrupt()
        }
        
        client?.close()
        super.stop()
    }

    /**
     * 로그 이벤트를 처리하는 메서드
     * 설정된 최소 로그 레벨 이상의 로그만 Discord로 전송합니다
     */
    override fun append(event: ILoggingEvent) {
        // 클라이언트가 초기화되지 않았으면 무시
        if (client == null) {
            return
        }
        
        // 설정된 최소 로그 레벨보다 낮은 로그 이벤트는 무시
        if (!isLevelEligible(event)) {
            return
        }

        try {
            val message = event.formattedMessage
            
            // Swagger 로그 확인 (컨트롤러 API 매핑 정보)
            if (isSwaggerLog(event)) {
                handleSwaggerLog(event)
                return
            }
            
            // 예외 정보를 포함하는 경우 강제로 파일 첨부 방식 사용
            val hasException = event.throwableProxy != null
            
            if (hasException || (message.length > maxMessageLength && enableFileAttachment)) {
                // 긴 메시지나 예외는 파일로 전송하기 위해 큐에 추가
                queueFileMessage(event)
            } else {
                // 일반 메시지는 임베드로 전송하기 위해 큐에 추가
                queueEmbedMessage(event)
            }
        } catch (e: Exception) {
            addError("Failed to queue log message", e)
        }
    }
    
    /**
     * Swagger 로그인지 확인
     */
    private fun isSwaggerLog(event: ILoggingEvent): Boolean {
        val message = event.formattedMessage
        // Swagger 로그 패턴 확인 (컨트롤러 API 매핑 정보)
        return message.contains("{GET [") || 
               message.contains("{POST [") || 
               message.contains("{PUT [") || 
               message.contains("{DELETE [") ||
               message.contains("{PATCH [") ||
               message.contains("{OPTIONS [") ||
               message.contains("{HEAD [") ||
               (event.loggerName.contains(".controller.") && 
                message.contains("]}: "))
    }
    
    /**
     * Swagger 로그 처리
     */
    private fun handleSwaggerLog(event: ILoggingEvent) {
        val currentTime = System.currentTimeMillis()
        val message = event.formattedMessage
        
        synchronized(swaggerLogsBuffer) {
            // 첫 번째 로그이거나 시간 간격이 길면 버퍼 초기화
            if (swaggerLogsBuffer.isEmpty() || 
                currentTime - lastSwaggerLogTimestamp > SWAGGER_LOG_TIMEOUT) {
                swaggerLogsBuffer.clear()
                swaggerLogsBuffer.appendLine(event.loggerName)
            }
            
            // 로그 추가
            swaggerLogsBuffer.appendLine(message)
            lastSwaggerLogTimestamp = currentTime
        }
    }
    
    /**
     * 수집된 Swagger 로그 처리
     */
    private fun processSwaggerLogs() {
        synchronized(swaggerLogsBuffer) {
            if (swaggerLogsBuffer.isNotEmpty() && 
                System.currentTimeMillis() - lastSwaggerLogTimestamp >= SWAGGER_LOG_TIMEOUT) {
                
                val content = swaggerLogsBuffer.toString()
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val fileName = "${timestamp}_SWAGGER_API_MAPPINGS.txt"
                
                val embed = WebhookEmbedBuilder()
                    .setColor(Color.CYAN.rgb)
                    .setTitle(WebhookEmbed.EmbedTitle("[$environment] API 매핑 정보", null))
                    .setDescription("스웨거 API 매핑 정보가 파일로 첨부되었습니다.")
                    .addField(WebhookEmbed.EmbedField(true, "Application", applicationName))
                    .setFooter(WebhookEmbed.EmbedFooter("Swagger API Mappings", null))
                    .setTimestamp(Instant.now())
                    .build()
                
                val contentBytes = content.toByteArray(StandardCharsets.UTF_8)
                messageQueue.add(LogMessage.FileMessage(embed, fileName, contentBytes))
                
                // 버퍼 비우기
                swaggerLogsBuffer.clear()
            }
        }
    }
    
    /**
     * 임베드 메시지를 큐에 추가
     */
    private fun queueEmbedMessage(event: ILoggingEvent) {
        val embed = createEmbed(event)
        messageQueue.add(LogMessage.EmbedMessage(embed))
    }
    
    /**
     * 파일 첨부 메시지를 큐에 추가
     */
    private fun queueFileMessage(event: ILoggingEvent) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val logLevel = event.level.toString()
        val fileName = "${timestamp}_${logLevel}_log.txt"
        
        val logContent = buildLogContent(event)
        val contentBytes = logContent.toByteArray(StandardCharsets.UTF_8)
        
        val embed = createSummaryEmbed(event)
        messageQueue.add(LogMessage.FileMessage(embed, fileName, contentBytes))
    }
    
    /**
     * 메시지 큐를 처리하는 메서드
     */
    private fun processMessageQueue() {
        if (isProcessing.getAndSet(true)) {
            return  // 이미 처리 중이면 리턴
        }
        
        try {
            // 속도 제한 상태이면 대기
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRateLimitTime < currentBackoffTime) {
                return
            }
            
            // 정규 분포 지터 추가 (예측성 감소 및 재시도 패턴 분산)
            if (messageQueue.isNotEmpty() && consecutiveRateLimits > 0) {
                val jitter = ThreadLocalRandom.current().nextLong(100, 500)
                Thread.sleep(jitter)
            }
            
            var processedCount = 0
            while (messageQueue.isNotEmpty() && processedCount < batchSize) {
                val message = messageQueue.poll() ?: break
                
                try {
                    when (message) {
                        is LogMessage.EmbedMessage -> {
                            sendWithRetry { client?.send(message.embed) }
                        }
                        is LogMessage.FileMessage -> {
                            val logStream = ByteArrayInputStream(message.content)
                            val messageBuilder = WebhookMessageBuilder()
                                .addEmbeds(message.embed)
                                .addFile(message.fileName, logStream)
                            
                            sendWithRetry { client?.send(messageBuilder.build()) }
                        }
                    }
                    
                    processedCount++
                    
                    // 속도 제한 방지를 위한 대기 (성공 시 점진적으로 백오프 시간 감소)
                    if (consecutiveRateLimits > 0) {
                        consecutiveRateLimits--
                        currentBackoffTime = Math.max(baseBackoffTime, currentBackoffTime / 2)
                    }
                    
                    // 메시지 사이에 지터를 포함한 대기
                    val jitter = ThreadLocalRandom.current().nextLong(100, 300)
                    Thread.sleep(waitBetweenMessages + jitter)
                } catch (e: Exception) {
                    // 속도 제한(429) 감지
                    if (e.message?.contains("429") == true || (e.cause?.message?.contains("429") == true)) {
                        handleRateLimit()
                        // 실패한 메시지는 다시 큐에 추가
                        messageQueue.add(message)
                        break
                    } else {
                        addError("Failed to send message to Discord: ${e.message}", e)
                        
                        // 일반 오류는 재시도 횟수 내에서만 큐에 다시 추가
                        if (e.message?.contains("Socket closed") == true || 
                            e.message?.contains("connection") == true) {
                            messageQueue.add(message)
                        }
                        break
                    }
                }
            }
        } finally {
            isProcessing.set(false)
        }
    }
    
    /**
     * 속도 제한(429) 처리
     */
    private fun handleRateLimit() {
        consecutiveRateLimits++
        lastRateLimitTime = System.currentTimeMillis()
        
        // 지수 백오프 적용
        currentBackoffTime = (currentBackoffTime * (1.5 + ThreadLocalRandom.current().nextDouble(0.1))).toLong()
        currentBackoffTime = Math.min(currentBackoffTime, maxBackoffTime)
        
        // 너무 많은 연속 속도 제한 시 로그 제거 고려
        if (consecutiveRateLimits > 10 && messageQueue.size > 50) {
            addWarn("Too many rate limits, dropping non-critical logs to prevent overflow")
            pruneNonCriticalLogs()
        }
        
        addWarn("Rate limited by Discord, backing off for $currentBackoffTime ms (consecutive: $consecutiveRateLimits)")
    }
    
    /**
     * 중요하지 않은 로그 제거
     */
    private fun pruneNonCriticalLogs() {
        val originalSize = messageQueue.size
        val prunedQueue = ConcurrentLinkedQueue<LogMessage>()
        
        // ERROR 레벨 로그만 유지
        messageQueue.forEach { message ->
            when (message) {
                is LogMessage.EmbedMessage -> {
                    if (message.embed.title?.text?.contains("ERROR") == true) {
                        prunedQueue.add(message)
                    }
                }
                is LogMessage.FileMessage -> {
                    if (message.embed.title?.text?.contains("ERROR") == true) {
                        prunedQueue.add(message)
                    }
                }
            }
        }
        
        val prunedSize = originalSize - prunedQueue.size
        messageQueue.clear()
        messageQueue.addAll(prunedQueue)
        addWarn("Pruned $prunedSize non-critical logs. Remaining: ${prunedQueue.size}")
    }
    
    /**
     * 재시도 로직을 포함한 전송 메서드
     */
    private fun sendWithRetry(sender: () -> Unit) {
        var attempts = 0
        var lastException: Exception? = null
        
        while (attempts < retryCount) {
            try {
                sender()
                return
            } catch (e: Exception) {
                lastException = e
                attempts++
                
                if (e.message?.contains("429") == true || (e.cause?.message?.contains("429") == true)) {
                    // 429 오류 발생 시 handleRateLimit()에서 처리할 수 있도록 바로 전파
                    throw e
                } else if (attempts < retryCount) {
                    // 지수 백오프 적용
                    val backoffTime = baseBackoffTime * (1 shl (attempts - 1)) + 
                                     ThreadLocalRandom.current().nextLong(100, 500)
                    Thread.sleep(Math.min(backoffTime, maxBackoffTime))
                }
            }
        }
        
        // 모든 재시도 실패
        throw lastException ?: IllegalStateException("Failed to send message after $retryCount attempts")
    }

    /**
     * 긴 로그 메시지를 파일로 첨부해서 전송합니다
     */
    private fun sendAsFile(event: ILoggingEvent) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val logLevel = event.level.toString()
        val fileName = "${timestamp}_${logLevel}_log.txt"
        
        val logContent = buildLogContent(event)
        val logStream = ByteArrayInputStream(logContent.toByteArray(StandardCharsets.UTF_8))
        
        val embed = createSummaryEmbed(event)
        
        val messageBuilder = WebhookMessageBuilder()
            .addEmbeds(embed)
            .addFile(fileName, logStream)
        
        client?.send(messageBuilder.build())
    }
    
    /**
     * 파일 첨부 시 요약된 임베드를 생성합니다
     */
    private fun createSummaryEmbed(event: ILoggingEvent): WebhookEmbed {
        val level = event.level.toString()
        val color = getColorForLevel(level)
        val timestamp = Instant.ofEpochMilli(event.timeStamp)
        
        // MDC에서 requestId 가져오기
        val requestId = event.mdcPropertyMap["requestId"] ?: "NONE"
        
        val summary = event.formattedMessage.let {
            if (it.length > 100) it.substring(0, 97) + "..." else it
        }
        
        return WebhookEmbedBuilder()
            .setColor(color)
            .setTitle(WebhookEmbed.EmbedTitle("[$environment] $level: ${event.loggerName}", null))
            .setDescription("로그가 첨부 파일로 제공됩니다. 요약: $summary")
            .addField(WebhookEmbed.EmbedField(true, "Application", applicationName))
            .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
            .addField(WebhookEmbed.EmbedField(true, "RequestId", requestId))
            .setFooter(WebhookEmbed.EmbedFooter("Logback Discord Appender", null))
            .setTimestamp(timestamp)
            .build()
    }
    
    /**
     * 로그 이벤트의 전체 내용을 구성합니다
     */
    private fun buildLogContent(event: ILoggingEvent): String {
        val sb = StringBuilder()
        sb.appendLine("=== 로그 상세 정보 ===")
        sb.appendLine("시간: ${Instant.ofEpochMilli(event.timeStamp)}")
        sb.appendLine("레벨: ${event.level}")
        sb.appendLine("로거: ${event.loggerName}")
        sb.appendLine("스레드: ${event.threadName}")
        sb.appendLine("애플리케이션: $applicationName")
        sb.appendLine("환경: $environment")
        
        // MDC에서 requestId 가져오기
        val requestId = event.mdcPropertyMap["requestId"] ?: "NONE"
        sb.appendLine("요청 ID: $requestId")
        
        sb.appendLine("===== 메시지 =====")
        
        // SQL 쿼리나 JSON인 경우 코드 블록으로 포맷팅 (파일 내용에서도 적용)
        val formattedMessage = formatMessageContent(event.formattedMessage)
        sb.appendLine(formattedMessage)
        
        // 예외가 있는 경우 스택트레이스 추가
        if (event.throwableProxy != null) {
            try {
                appendThrowableInfo(sb, event.throwableProxy, "")
            } catch (e: Exception) {
                // 예외 정보 추출 중 오류 발생 시 기본 예외 정보만 추가
                sb.appendLine("\n===== 스택트레이스 추출 오류 =====")
                sb.appendLine("예외 정보를 추출하는 중 오류가 발생했습니다: ${e.message}")
                
                // 원래 예외 메시지는 최소한 포함
                sb.appendLine("\n예외 타입: ${event.throwableProxy?.className}")
                sb.appendLine("예외 메시지: ${event.throwableProxy?.message}")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 예외 정보 추출 및 포맷팅 (개선된 버전)
     */
    private fun appendThrowableInfo(sb: StringBuilder, throwable: IThrowableProxy?, prefix: String) {
        if (throwable == null) return
        
        sb.appendLine("\n${prefix}===== 스택트레이스 =====")
        sb.appendLine("${prefix}예외 타입: ${throwable.className}")
        sb.appendLine("${prefix}예외 메시지: ${throwable.message}")
        
        sb.appendLine("\n${prefix}----- 스택트레이스 상세 -----")
        val stackTraceContent = throwable.stackTraceElementProxyArray.joinToString("\n") { 
            "${prefix}${it}"
        }
        sb.appendLine(stackTraceContent)
        
        // 중첩된 원인 예외 처리
        throwable.cause?.let { 
            sb.appendLine()
            appendThrowableInfo(sb, it, "$prefix  ")
        }
        
        // 억제된 예외 처리
        throwable.suppressed?.forEach { suppressed ->
            sb.appendLine()
            sb.appendLine("${prefix}----- 억제된 예외 -----")
            appendThrowableInfo(sb, suppressed, "$prefix  ")
        }
    }

    /**
     * 로그 이벤트를 Discord 임베드 형식으로 변환합니다
     */
    private fun createEmbed(event: ILoggingEvent): WebhookEmbed {
        val level = event.level.toString()
        val color = getColorForLevel(level)
        val timestamp = Instant.ofEpochMilli(event.timeStamp)
        
        // MDC에서 requestId 가져오기
        val requestId = event.mdcPropertyMap["requestId"] ?: "NONE"
        
        // SQL 문 포맷팅 (코드 블록으로 감싸기)
        val formattedMessage = formatMessageContent(event.formattedMessage)
        
        return WebhookEmbedBuilder()
            .setColor(color)
            .setTitle(WebhookEmbed.EmbedTitle("[$environment] $level: ${event.loggerName}", null))
            .setDescription(formattedMessage)
            .addField(WebhookEmbed.EmbedField(true, "Application", applicationName))
            .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
            .addField(WebhookEmbed.EmbedField(true, "RequestId", requestId))
            .setFooter(WebhookEmbed.EmbedFooter("Logback Discord Appender", null))
            .setTimestamp(timestamp)
            .build()
    }

    // Setter 메서드들 (XML 설정에서 사용됨)
    fun setApplicationName(applicationName: String) {
        this.applicationName = applicationName
    }

    fun setEnvironment(environment: String) {
        this.environment = environment
    }

    fun setMinLevel(minLevel: String) {
        this.minLevel = minLevel
    }
    
    fun setMaxMessageLength(maxMessageLength: Int) {
        this.maxMessageLength = maxMessageLength
    }
    
    fun setEnableFileAttachment(enableFileAttachment: Boolean) {
        this.enableFileAttachment = enableFileAttachment
    }
    
    fun setWaitBetweenMessages(waitBetweenMessages: Long) {
        this.waitBetweenMessages = waitBetweenMessages
    }
    
    fun setBatchSize(batchSize: Int) {
        this.batchSize = batchSize
    }
    
    fun setRetryCount(retryCount: Int) {
        this.retryCount = retryCount
    }
    
    fun setBaseBackoffTime(baseBackoffTime: Long) {
        this.baseBackoffTime = baseBackoffTime
    }
    
    fun setMaxBackoffTime(maxBackoffTime: Long) {
        this.maxBackoffTime = maxBackoffTime
    }
} 