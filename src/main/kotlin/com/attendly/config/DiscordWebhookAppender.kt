package com.attendly.config

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.send.WebhookEmbed
import club.minnced.discord.webhook.send.WebhookEmbedBuilder
import java.awt.Color
import java.time.Instant

/**
 * Discord 웹훅으로 로그를 전송하는 Logback 어펜더
 */
class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {
    private var webhookUrl: String? = null
    private var client: WebhookClient? = null
    private var applicationName: String = "Attendly API"
    private var environment: String = "UNKNOWN"
    private var minLevel: String = "WARN"

    /**
     * 어펜더가 시작될 때 호출되는 메서드
     * 웹훅 클라이언트를 초기화합니다
     */
    override fun start() {
        if (webhookUrl.isNullOrBlank()) {
            addWarn("No webhookUrl set for DiscordWebhookAppender. Discord logging will be disabled.")
            // 웹훅 URL이 없더라도 어펜더는 시작하여 다른 로깅에 영향을 주지 않음
            super.start()
            return
        }
        
        // 안전하게 로컬 변수로 복사하여 스마트 캐스트 문제 해결
        val url = webhookUrl
        if (url != null) {
            try {
                client = WebhookClient.withUrl(url)
                super.start()
            } catch (e: Exception) {
                addError("Failed to initialize Discord webhook client", e)
                // 예외가 발생해도 어펜더는 시작
                super.start()
            }
        }
    }

    /**
     * 어펜더가 종료될 때 호출되는 메서드
     * 웹훅 클라이언트 리소스를 해제합니다
     */
    override fun stop() {
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
            client?.send(createEmbed(event))
        } catch (e: Exception) {
            addError("Failed to send log to Discord webhook", e)
        }
    }

    /**
     * 로그 이벤트를 Discord 임베드 형식으로 변환합니다
     */
    private fun createEmbed(event: ILoggingEvent): WebhookEmbed {
        val level = event.level.toString()
        val color = getColorForLevel(level)
        val timestamp = Instant.ofEpochMilli(event.timeStamp)
        
        return WebhookEmbedBuilder()
            .setColor(color)
            .setTitle(WebhookEmbed.EmbedTitle("[$environment] $level: ${event.loggerName}", null))
            .setDescription(event.formattedMessage)
            .addField(WebhookEmbed.EmbedField(true, "Application", applicationName))
            .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
            .setFooter(WebhookEmbed.EmbedFooter("Logback Discord Appender", null))
            .setTimestamp(timestamp)
            .build()
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

    // Setter 메서드들 (XML 설정에서 사용됨)
    fun setWebhookUrl(webhookUrl: String) {
        this.webhookUrl = webhookUrl
    }

    fun setApplicationName(applicationName: String) {
        this.applicationName = applicationName
    }

    fun setEnvironment(environment: String) {
        this.environment = environment
    }

    fun setMinLevel(minLevel: String) {
        this.minLevel = minLevel
    }
} 