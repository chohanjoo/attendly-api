# API 요청 추적을 위한 requestId 로깅 구현

## 문제 상황

API 호출 시 발생하는 로그를 추적하기 어려운 문제가 있었습니다. 특히:

1. 여러 요청이 동시에 처리될 때 특정 요청에 대한 로그 흐름을 파악하기 어려웠습니다.
2. 클라이언트에서 발생한 문제를 서버 로그에서 추적할 방법이 없었습니다.
3. 로그 검색 시 특정 요청의 전체 흐름을 확인하기 위한 연결 정보가 부족했습니다.

## 개선 내용

API 요청마다 고유한 requestId를 부여하고, 모든 로그에 이 ID를 포함하도록 개선했습니다.

### 1. ApiLogInterceptor 개선

```kotlin
companion object {
    const val X_REQUEST_ID = "X-Request-ID"
    const val REQUEST_ID_ATTRIBUTE = "requestId"
}

override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    // 헤더에서 requestId를 가져오거나 없으면 생성
    val requestId = request.getHeader(X_REQUEST_ID) ?: UUID.randomUUID().toString()
    
    requestMap[requestId] = System.currentTimeMillis()
    request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId)
    
    // 응답 헤더에도 requestId 추가
    response.addHeader(X_REQUEST_ID, requestId)
    
    // MDC에 requestId 설정하여 로깅에 활용
    MDC.put(REQUEST_ID_ATTRIBUTE, requestId)
    
    return true
}
```

주요 변경사항:
- 클라이언트가 제공한 `X-Request-ID` 헤더값을 사용
- 헤더가 없는 경우 UUID를 생성하여 requestId로 사용
- 응답 헤더에 동일한 requestId를 포함시켜 클라이언트에서도 추적 가능하도록 함
- SLF4J의 MDC(Mapped Diagnostic Context)를 활용하여 로깅 시스템에 requestId 전달
- 요청 처리 완료 후 MDC에서 requestId 제거

### 2. 로그 패턴 수정 (logback-spring.xml)

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{requestId:-NONE}] %-5level %logger{36} - %msg%n</pattern>
```

주요 변경사항:
- 모든 로그 출력 패턴에 requestId 정보 추가 (`[%X{requestId:-NONE}]`)
- requestId가 없는 경우 기본값으로 "NONE" 출력

### 3. 디스코드 로깅에 requestId 추가 (DiscordWebhookAppender.kt)

```kotlin
// Discord 임베드 생성 시 requestId 필드 추가
private fun createEmbed(event: ILoggingEvent): WebhookEmbed {
    val level = event.level.toString()
    val color = getColorForLevel(level)
    val timestamp = Instant.ofEpochMilli(event.timeStamp)
    
    // MDC에서 requestId 가져오기
    val requestId = event.mdcPropertyMap["requestId"] ?: "NONE"
    
    return WebhookEmbedBuilder()
        .setColor(color)
        .setTitle(WebhookEmbed.EmbedTitle("[$environment] $level: ${event.loggerName}", null))
        .setDescription(event.formattedMessage)
        .addField(WebhookEmbed.EmbedField(true, "Application", applicationName))
        .addField(WebhookEmbed.EmbedField(true, "Thread", event.threadName))
        .addField(WebhookEmbed.EmbedField(true, "RequestId", requestId))
        .setFooter(WebhookEmbed.EmbedFooter("Logback Discord Appender", null))
        .setTimestamp(timestamp)
        .build()
}

// 로그 파일 첨부 시에도 requestId 포함
private fun buildLogContent(event: ILoggingEvent): String {
    val sb = StringBuilder()
    // ...기존 코드...
    
    // MDC에서 requestId 가져오기
    val requestId = event.mdcPropertyMap["requestId"] ?: "NONE"
    sb.appendLine("요청 ID: $requestId")
    
    // ...기존 코드...
}
```

주요 변경사항:
- Discord로 전송되는 모든 로그 메시지에 requestId 필드 추가
- 임베드 형식 및 파일 첨부 형식 모두에 requestId 포함
- 로그 첨부 파일의 내용에도 requestId 정보 추가

### 4. Swagger 문서에 requestId 헤더 추가

```kotlin
@Bean
fun openAPI(): OpenAPI {
    val securitySchemeName = "bearerAuth"
    
    return OpenAPI()
        .info(apiInfo())
        .components(
            Components()
                // ... 기존 코드 ...
                .addParameters(
                    ApiLogInterceptor.X_REQUEST_ID,
                    Parameter()
                        .name(ApiLogInterceptor.X_REQUEST_ID)
                        .description("요청 추적을 위한 고유 ID (제공하지 않으면 서버에서 자동 생성)")
                        .`in`("header")
                        .required(false)
                        .example("550e8400-e29b-41d4-a716-446655440000")
                )
        )
        // ... 기존 코드 ...
}
```

주요 변경사항:
- OpenAPI 명세에 `X-Request-ID` 헤더 파라미터 추가
- 모든 API 엔드포인트에 requestId 헤더 문서화
- 클라이언트가 이 헤더를 사용하는 방법에 대한 설명 제공

## 기대효과

1. **로그 추적성 향상**: 모든 로그에 requestId가 포함되어 특정 요청의 전체 흐름을 쉽게 추적 가능
2. **클라이언트-서버 연계 강화**: 클라이언트는 자체 requestId를 사용하여 요청을 보내고, 서버 로그와 연계 가능
3. **장애 대응 개선**: 문제 발생 시 해당 요청 ID를 기준으로 관련 로그를 빠르게 검색 가능
4. **분산 시스템 추적 용이**: 마이크로서비스 환경에서 여러 서비스 간 요청 흐름 추적에 활용 가능
5. **API 문서화 개선**: Swagger UI에서 requestId 헤더 사용법을 확인할 수 있어 API 사용자의 이해도 향상
6. **디스코드 알림 개선**: 중요 오류 발생 시 디스코드로 전송되는 알림에 requestId를 포함하여 문제 추적 용이

## 적용 방법

### 서버 측
- 별도 설정 변경 없이 자동으로 모든 API 요청에 적용됨
- 기존 requestId가 없는 요청은 자동으로 UUID 생성

### 클라이언트 측
- API 요청 시 `X-Request-ID` 헤더에 고유 ID 포함 가능
- 응답 헤더에서 동일한 `X-Request-ID` 확인 가능
- 로그나 에러 보고 시 이 ID를 포함하면 서버 로그 추적 용이
- Swagger UI에서 헤더 정보 확인 및 테스트 가능

## 테스트 결과

단위 테스트를 통해 다음 사항을 검증했습니다:
1. 헤더에 requestId가 없을 때 자동 생성되는지 확인
2. 헤더에 requestId가 있을 때 그대로 사용되는지 확인
3. 요청 처리 후 MDC에서 requestId가 제거되는지 확인
4. 로그에 requestId가 정상적으로 포함되는지 확인
5. 디스코드 알림에 requestId가 포함되는지 확인

## 결론

이번 개선을 통해 API 요청 추적성을 크게 향상시켰습니다. 특히 장애 상황 발생 시 문제 원인 파악에 소요되는 시간을 줄이고, 클라이언트-서버 간 로그 연계를 용이하게 하여 전체적인 애플리케이션 관리와 디버깅 효율을 높였습니다. 또한 Swagger 문서에 requestId 헤더를 추가하여 API 사용자들도 쉽게 이 기능을 활용할 수 있게 되었습니다. 