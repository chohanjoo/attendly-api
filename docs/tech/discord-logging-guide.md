# Discord 로깅 시스템 가이드

## 개요

Attendly API는 시스템의 중요 로그 이벤트를 Discord로 전송하는 기능을 제공합니다. 이 문서는 Discord 로깅 시스템의 설계, 구현 방법, 사용법에 대해 설명합니다.

## 목차

1. [시스템 아키텍처](#1-시스템-아키텍처)
2. [설정 방법](#2-설정-방법)
3. [코드에서 사용하기](#3-코드에서-사용하기)
4. [테스트 방법](#4-테스트-방법)
5. [실제 적용 사례](#5-실제-적용-사례)
6. [문제 해결](#6-문제-해결)
7. [확장 및 개선 방안](#7-확장-및-개선-방안)

## 1. 시스템 아키텍처

Discord 로깅 시스템은 다음과 같은 컴포넌트로 구성됩니다:

### 1.1 주요 컴포넌트

- **DiscordWebhookAppender**: Logback 프레임워크의 AppenderBase를 확장한 커스텀 어펜더
- **DiscordLogger**: 로그 메시지를 쉽게 생성할 수 있는 유틸리티 클래스
- **DiscordLogConfig**: 애플리케이션 시작 시 로그 테스트 및 초기화를 담당하는 설정 클래스
- **logback-spring.xml**: Discord 어펜더 설정을 포함하는 로그백 설정 파일

### 1.2 동작 원리

```
[애플리케이션 코드] → DiscordLogger → SLF4J → Logback → DiscordWebhookAppender → Discord 웹훅 API
```

1. 애플리케이션 코드에서 `DiscordLogger`를 통해 로그 메시지 생성
2. SLF4J와 Logback이 로그 이벤트를 처리
3. 설정된 로그 레벨(기본적으로 WARN 이상)에 해당하는 로그만 `DiscordWebhookAppender`로 전달
4. Discord 웹훅 API를 통해 메시지를 Discord 채널로 전송

## 2. 설정 방법

### 2.1 Discord 웹훅 생성

1. Discord 서버에서 관리자 권한으로 서버 설정 → 연동 → 웹훅 메뉴로 이동
2. "새 웹훅" 버튼 클릭
3. 웹훅의 이름과 아이콘 설정
4. 메시지를 보낼 채널 선택
5. "웹훅 URL 복사" 클릭하여 URL 저장

### 2.2 환경 변수 설정

애플리케이션에서 Discord 웹훅 URL을 사용하도록 환경 변수를 설정합니다:

```bash
# 로컬 개발 환경에서 설정
export DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your-webhook-url

# Docker Compose에서 설정
# docker-compose.yml 파일 예시:
services:
  app:
    environment:
      - DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your-webhook-url
```

### 2.3 애플리케이션 설정 파일에서 직접 설정

`application.yml` 파일에서 Discord 웹훅 설정을 구성할 수 있습니다:

```yaml
# application.yml
discord:
  webhook:
    url: ${DISCORD_WEBHOOK_URL:}  # 환경 변수 참조, 없으면 빈 문자열
    min-level: WARN  # 최소 로그 레벨 (INFO, WARN, ERROR 중 선택)
```

### 2.4 Logback 설정

Discord 어펜더는 `logback-spring.xml` 파일에서 구성됩니다:

```xml
<!-- Discord 웹훅 로깅 설정 -->
<appender name="DISCORD" class="com.attendly.config.DiscordWebhookAppender">
    <webhookUrl>${DISCORD_WEBHOOK_URL:-}</webhookUrl>
    <applicationName>Attendly API</applicationName>
    <environment>${SPRING_PROFILES_ACTIVE:-local}</environment>
    <minLevel>WARN</minLevel>
    <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
        <level>WARN</level>
    </filter>
</appender>

<!-- 비동기 Discord 앱엔더 -->
<appender name="ASYNC_DISCORD" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="DISCORD" />
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <includeCallerData>true</includeCallerData>
</appender>

<!-- 루트 로거에 Discord 어펜더 추가 -->
<root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
    <appender-ref ref="ASYNC_DISCORD" />
</root>
```

## 3. 코드에서 사용하기

### 3.1 기본 사용법

`DiscordLogger` 유틸리티 클래스를 사용하여 다양한 로그 레벨의 메시지를 생성할 수 있습니다:

```kotlin
import com.attendly.util.DiscordLogger

// 정보 로깅 (INFO 레벨 - 기본적으로 Discord로 전송되지 않음)
DiscordLogger.info("중요 정보 메시지")

// 경고 로깅 (WARN 레벨 - Discord로 전송됨)
DiscordLogger.warn("경고 메시지")

// 오류 로깅 (ERROR 레벨 - Discord로 전송됨)
DiscordLogger.error("오류 메시지")

// 예외와 함께 오류 로깅
try {
    // 오류 발생 가능한 코드
} catch (e: Exception) {
    DiscordLogger.error("예외 발생", e)
}
```

### 3.2 특화된 이벤트 로깅

특정 유형의 이벤트를 명확하게 구분하여 로깅할 수 있습니다:

```kotlin
// 보안 관련 이벤트 로깅
DiscordLogger.securityEvent("비정상적인 로그인 시도 감지")
DiscordLogger.securityEvent("CSRF 토큰 불일치", isError = true)  // ERROR 레벨로 로깅

// 시스템 이벤트 로깅
DiscordLogger.systemEvent("배치 작업 시작")

// 인증 이벤트 로깅
DiscordLogger.authEvent("로그인 성공", userId = "user123")
DiscordLogger.authEvent("로그인 실패 (잘못된 자격 증명)", userId = "unknown_user")
```

### 3.3 애플리케이션 시작 시 Discord 로깅

`DiscordLogConfig` 클래스는 애플리케이션 시작 시 로그 이벤트를 생성합니다:

```kotlin
@Configuration
class DiscordLogConfig(private val env: Environment) {
    
    @Bean
    fun discordStartupLogger(): CommandLineRunner {
        return CommandLineRunner {
            val activeProfiles = env.activeProfiles.joinToString()
            val applicationName = env.getProperty("spring.application.name", "Attendly API")
            
            // 애플리케이션 시작 이벤트를 Discord에 로깅
            DiscordLogger.systemEvent("애플리케이션 시작: $applicationName (프로필: $activeProfiles)")
        }
    }
}
```

## 4. 테스트 방법

### 4.1 API를 통한 테스트

테스트 API 엔드포인트를 사용하여 Discord 로깅을 테스트할 수 있습니다:

```bash
# 경고 로그 테스트 (Discord로 전송됨)
curl -X GET "http://localhost:8080/api/test/logging/warn/테스트메시지"

# 오류 로그 테스트 (Discord로 전송됨)
curl -X GET "http://localhost:8080/api/test/logging/error/테스트메시지"

# 예외 로그 테스트 (Discord로 전송됨)
curl -X GET "http://localhost:8080/api/test/logging/exception/테스트메시지"

# 보안 이벤트 로그 테스트 (Discord로 전송됨)
curl -X GET "http://localhost:8080/api/test/logging/security/테스트메시지"
```

### 4.2 테스트 프로필을 사용한 테스트

`test-discord-log` 프로필을 활성화하여 애플리케이션 시작 시 테스트 로그 메시지를 자동으로 생성할 수 있습니다:

```bash
# 테스트 프로필 활성화
java -jar attendly-api.jar --spring.profiles.active=test-discord-log
```

이 프로필이 활성화되면 `DiscordLogConfig.discordLogTester()` 메서드가 다음과 같은 로그 메시지를 생성합니다:
- INFO 레벨 메시지 (Discord로 전송되지 않음)
- WARN 레벨 메시지 (Discord로 전송됨)
- ERROR 레벨 메시지 및 예외 (Discord로 전송됨)

### 4.3 단위 테스트

`DiscordLogger` 클래스의 단위 테스트는 `DiscordLoggerTest` 클래스에서 확인할 수 있습니다. 이 테스트는 `ListAppender`를 사용하여 실제 Discord 전송 없이 로그 메시지가 올바르게 생성되는지 검증합니다.

## 5. 실제 적용 사례

### 5.1 로그인 이벤트 로깅

인증 관련 이벤트를 Discord로 로깅하는 예시:

```kotlin
@Component
class AuthEventLogger {

    @EventListener
    fun onAuthenticationSuccess(event: AuthenticationSuccessEvent) {
        val username = event.authentication.name
        
        // 일반 로그인 성공은 INFO 레벨 (Discord로 전송되지 않음)
        DiscordLogger.info("사용자 로그인 성공: $username")
        
        // 관리자 로그인은 WARN 레벨로 로깅 (Discord로 전송됨)
        if (event.authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
            DiscordLogger.authEvent("관리자 로그인", username)
        }
    }
    
    @EventListener
    fun onAuthenticationFailure(event: AuthenticationFailureBadCredentialsEvent) {
        val username = event.authentication.name
        
        // 로그인 실패는 보안 이벤트로 로깅 (Discord로 전송됨)
        DiscordLogger.authEvent("로그인 실패 (잘못된 자격 증명)", username)
    }
}
```

### 5.2 중요 서비스 작업 로깅

중요한 서비스 작업이나 시스템 이벤트를 Discord로 로깅할 수 있습니다:

```kotlin
@Service
class UserService(/* 의존성 */) {
    
    fun createUser(userDto: UserDto): User {
        try {
            // 사용자 생성 로직
            val user = userRepository.save(userDto.toEntity())
            
            // 관리자 계정 생성은 중요 이벤트로 로깅
            if (user.role == Role.ADMIN) {
                DiscordLogger.systemEvent("새 관리자 계정 생성: ${user.email}")
            }
            
            return user
        } catch (e: Exception) {
            DiscordLogger.error("사용자 생성 실패: ${userDto.email}", e)
            throw e
        }
    }
    
    fun deleteUser(userId: Long) {
        try {
            val user = userRepository.findById(userId).orElseThrow()
            userRepository.delete(user)
            
            // 사용자 삭제는 중요 이벤트로 로깅
            DiscordLogger.systemEvent("사용자 삭제: ${user.email} (ID: $userId)")
        } catch (e: Exception) {
            DiscordLogger.error("사용자 삭제 실패: $userId", e)
            throw e
        }
    }
}
```

## 6. 문제 해결

### 6.1 일반적인 문제

#### 로그 메시지가 Discord로 전송되지 않는 경우

1. **웹훅 URL 확인**
   - 환경 변수 `DISCORD_WEBHOOK_URL`이 올바르게 설정되었는지 확인
   - Discord 웹훅이 유효한지 확인 (만료되었거나 삭제되었을 수 있음)
   
2. **로그 레벨 확인**
   - INFO 레벨 로그는 기본적으로 Discord로 전송되지 않음
   - Discord로 전송하려면 WARN 이상의 로그 레벨 사용 (minLevel 설정 확인)

3. **네트워크 연결 확인**
   - 애플리케이션이 Discord 서버에 접근할 수 있는지 확인
   - 방화벽이나 프록시 설정 확인

#### 지연 시간이 긴 경우

1. **비동기 처리 확인**
   - `ASYNC_DISCORD` 어펜더가 올바르게 설정되었는지 확인
   - 큐 크기와 폐기 임계값 설정 확인

### 6.2 디버깅

로그 전송 문제를 디버깅하려면:

1. Logback의 내부 디버그 모드 활성화:
   ```xml
   <configuration debug="true">
       <!-- 기존 설정 -->
   </configuration>
   ```

2. DiscordWebhookAppender에서 발생하는 오류 확인:
   ```
   2023-05-01 12:34:56 [main] DEBUG ch.qos.logback.core.joran.action.AppenderAction - About to instantiate appender of type [com.attendly.config.DiscordWebhookAppender]
   ```

## 7. 확장 및 개선 방안

### 7.1 개선 가능한 부분

1. **레이트 리밋 처리**
   - Discord API에는 웹훅 요청에 대한 레이트 리밋이 있음
   - 많은 로그 이벤트가 있을 경우 레이트 리밋을 고려한 처리 필요

2. **로그 배치 처리**
   - 짧은 시간에 여러 로그를 그룹화하여 한 번에 전송
   - 로그 볼륨이 많을 때 효율적인 처리 가능

3. **더 풍부한 메시지 포맷**
   - 특정 유형의 이벤트에 대해 더 자세한 정보와 서식 제공
   - 버튼, 링크 등의 상호작용 요소 추가

### 7.2 확장 아이디어

1. **중요도에 따른 채널 분리**
   - 오류, 보안 이벤트, 시스템 이벤트 등 다른 채널로 분리
   - 여러 웹훅 URL 설정 지원

2. **알림 멘션 추가**
   - 심각한 오류 발생 시 @everyone 또는 특정 역할/사용자 멘션
   - 빠른 대응이 필요한 이벤트에 유용

3. **로그 필터링 향상**
   - 특정 패키지, 클래스 또는 메시지 패턴에 따른 필터링 지원
   - 로그 볼륨 조절을 위한 샘플링 기능

## 참고 자료

- [Discord Webhook API 문서](https://discord.com/developers/docs/resources/webhook)
- [Logback 공식 문서](http://logback.qos.ch/documentation.html)
- [SLF4J 공식 문서](http://www.slf4j.org/manual.html)
- [club.minnced:discord-webhooks 라이브러리](https://github.com/MinnDevelopment/discord-webhooks) 