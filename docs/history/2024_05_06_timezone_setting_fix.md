# 도커 컨테이너와 호스트 시간 불일치 문제 해결

## 문제 상황

애플리케이션을 도커 컨테이너로 실행했을 때 컨테이너 내부의 시간과 호스트 컴퓨터의 시간이 일치하지 않는 문제가 발생했습니다. 이로 인해 다음과 같은 문제가 발생할 수 있습니다:

1. 로그 타임스탬프가 실제 시간과 일치하지 않음
2. 데이터베이스에 기록되는 시간 정보가 부정확함
3. 예약 작업(배치 작업)이 의도한 시간에 실행되지 않음
4. 출석 기록 입력 기능에서 시간 제약 조건이 부정확하게 적용됨

기본적으로 도커 컨테이너는 UTC 시간대를 사용하는데, 한국에서 운영되는 애플리케이션의 경우 KST(한국 표준시)로 설정되어야 합니다.

## 원인 분석

문제의 원인은 다음과 같았습니다:

1. 도커 컨테이너의 기본 시간대가 UTC로 설정되어 있음
2. JDBC URL의 `serverTimezone` 파라미터가 UTC로 설정되어 있음
3. 애플리케이션에서 명시적인 시간대 설정이 없었음

## 해결 방안

### 1. Docker Compose 설정 변경

`docker-compose.yml` 파일에 양쪽 서비스(app, db)의 환경 변수로 `TZ: Asia/Seoul`을 추가하여 컨테이너의 시간대를 KST로 설정했습니다.

```yaml
services:
  db:
    image: mysql:8.0
    # ... 다른 설정들 ...
    environment:
      # ... 다른 환경 변수들 ...
      TZ: Asia/Seoul
    # ... 다른 설정들 ...

  app:
    build: .
    # ... 다른 설정들 ...
    environment:
      # ... 다른 환경 변수들 ...
      TZ: Asia/Seoul
    # ... 다른 설정들 ...
```

### 2. Dockerfile 수정

애플리케이션 `Dockerfile`에 타임존 설정 코드를 추가했습니다.

```dockerfile
FROM amazoncorretto:21
# ... 다른 설정들 ...

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ... 다른 설정들 ...
```

### 3. 데이터베이스 연결 설정 변경

모든 환경(개발, 프로덕션, 테스트)의 데이터베이스 연결 설정에서 시간대 관련 파라미터를 변경했습니다.

#### 개발 환경 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:p6spy:mysql://localhost:3306/attendly_api?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    # ... 다른 설정들 ...
```

#### 프로덕션 환경 (application-prod.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:church_attendly}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    # ... 다른 설정들 ...
```

#### 테스트 환경 (application-test.yml)

H2 데이터베이스는 URL 파라미터 형식이 다르기 때문에, 적절한 형식으로 시간대를 설정했습니다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;TIME_ZONE=Asia/Seoul
    # ... 다른 설정들 ...
```

#### Docker Compose의 환경 변수 수정

docker-compose.yml 파일의 SPRING_DATASOURCE_URL 환경 변수도 수정했습니다.

```yaml
services:
  app:
    # ... 다른 설정들 ...
    environment:
      # ... 다른 환경 변수들 ...
      SPRING_DATASOURCE_URL: jdbc:p6spy:mysql://db:3306/attendly_api?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    # ... 다른 설정들 ...
```

## 검증

변경 사항을 적용한 후, 다음 명령어로 컨테이너를 빌드하고 실행했습니다.

```bash
docker-compose build
docker-compose up -d
```

다음 명령어로 시간 설정이 제대로 적용되었는지 확인했습니다.

```bash
# 호스트 시간 확인
date
# 애플리케이션 컨테이너 시간 확인
docker exec attendly-api-app date
# 데이터베이스 컨테이너 시간 확인
docker exec attendly-api-mysql date
```

모든 시스템에서 동일한 시간(KST)이 표시되는 것을 확인했습니다.

## 결론 및 추가 고려사항

이번 수정으로 애플리케이션, 데이터베이스, 호스트 컴퓨터 간의 시간 동기화 문제를 해결했습니다. 이를 통해 로그 타임스탬프, 데이터베이스 시간 기록, 예약 작업 등이 의도한 대로 동작할 수 있게 되었습니다.

시간대 관련 설정은 배포 환경에 따라 달라질 수 있으므로, 다른 환경에 배포할 때 해당 시간대에 맞게 설정을 변경해야 할 수도 있습니다. 또한, 여러 지역에 서비스를 제공하는 경우 시간대 처리에 대한 더 복잡한 전략이 필요할 수 있습니다. 