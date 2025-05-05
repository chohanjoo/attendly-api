# Attendly API (교회 출석부 웹 서비스)

교회 대학부 리더들이 매주 조원(학생) 출석을 기록하고, 마을장·교역자가 데이터를 관리할 수 있는 웹 기반 출석 관리 시스템입니다.

## 기술 스택

- **언어**: Kotlin 1.9.x
- **Framework**: Spring Boot 3.2.x
- **ORM**: Spring Data JPA + Hibernate
- **DB**: MySQL 8.0
- **인증**: JWT (jjwt)
- **문서**: Springdoc OpenAPI 3
- **빌드**: Gradle (Kotlin DSL)
- **컨테이너**: Docker

## 개발 환경 설정

### 사전 요구사항

- JDK 21
- Docker 및 Docker Compose
- Git

### 프로젝트 설정

1. 저장소 클론
   ```bash
   git clone https://github.com/your-org/attendly-api.git
   cd attendly-api
   ```

2. 로컬 개발 서버 실행
   ```bash
   # MySQL 컨테이너 실행
   docker-compose up -d db
   
   # 애플리케이션 실행
   ./gradlew bootRun
   ```

3. 스웨거 UI 접속
   ```
   http://localhost:8080/swagger-ui.html
   ```

## Docker 실행 방법

```bash
# 전체 스택 (애플리케이션 + DB) 실행
docker-compose up -d

# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

## API 사용 예시

### 1. 로그인 (토큰 발급)

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "leader@church.com",
    "password": "leader123"
  }'
```

응답:
```json
{
  "userId": 4,
  "name": "이리더",
  "role": "LEADER",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 2. 출석 데이터 등록 (리더 권한)

```bash
curl -X POST http://localhost:8080/api/attendance \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "gbsId": 1,
    "weekStart": "2025-01-05",
    "attendances": [
      {
        "memberId": 5,
        "worship": "O",
        "qtCount": 5,
        "ministry": "A"
      },
      {
        "memberId": 6,
        "worship": "X",
        "qtCount": 2,
        "ministry": "C"
      }
    ]
  }'
```

### 3. 마을 출석 현황 조회 (마을장 권한)

```bash
curl -X GET "http://localhost:8080/api/village/1/attendance?weekStart=2025-01-05" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 4. 부서 통계 리포트 조회 (교역자 권한)

```bash
curl -X GET "http://localhost:8080/api/departments/1/report?startDate=2025-01-01&endDate=2025-01-31" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## 테스트 계정 정보

| 계정 이메일 | 비밀번호 | 역할 |
|------------|---------|-----|
| admin@church.com | admin123 | 관리자 |
| minister@church.com | minister123 | 교역자 |
| vleader@church.com | village123 | 마을장 |
| leader@church.com | leader123 | 리더 |

## 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다.

## Discord 로깅 설정 및 사용법

Attendly API는 중요 로그 이벤트를 Discord로 전송하는 기능을 제공합니다. 이 기능을 통해 시스템 오류, 보안 이벤트, 중요 애플리케이션 이벤트 등을 실시간으로 모니터링할 수 있습니다.

### 설정 방법

1. Discord 서버에서 웹훅 생성:
   - Discord 서버 설정 → 연동 → 웹훅 → 새 웹훅 생성
   - 웹훅의 이름과 아이콘 설정
   - 웹훅 URL 복사

2. 환경 변수 설정:
   ```bash
   export DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/your-webhook-url
   ```

3. 또는 `application.yml` 파일에 직접 설정:
   ```yaml
   discord:
     webhook:
       url: https://discord.com/api/webhooks/your-webhook-url
       min-level: WARN  # 최소 로그 레벨 (INFO, WARN, ERROR 중 선택)
   ```

### 코드에서 사용하기

```kotlin
import com.attendly.util.DiscordLogger

// 정보 로깅 (INFO 레벨 - 기본적으로 Discord로 전송되지 않음)
DiscordLogger.info("중요 정보 메시지")

// 경고 로깅 (WARN 레벨 - Discord로 전송됨)
DiscordLogger.warn("경고 메시지")

// 오류 로깅 (ERROR 레벨 - Discord로 전송됨)
DiscordLogger.error("오류 메시지")
DiscordLogger.error("예외 발생", exception)

// 특정 이벤트 타입으로 로깅
DiscordLogger.securityEvent("보안 이벤트 발생")
DiscordLogger.systemEvent("시스템 이벤트 발생")
DiscordLogger.authEvent("인증 이벤트", "사용자ID")
```

### 테스트하기

Discord 로깅이 제대로 작동하는지 테스트하려면 `test-discord-log` 프로필을 활성화하여 애플리케이션을 실행하세요:

```bash
java -jar attendly-api.jar --spring.profiles.active=test-discord-log
```

이렇게 실행하면 시작 시 테스트 로그 메시지가 Discord로 전송됩니다. 