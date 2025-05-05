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