# Spring Boot 프로파일(Profiles) 가이드

## 1. 프로파일이란?

프로파일은 Spring Boot 애플리케이션이 서로 다른 환경(개발, 테스트, 프로덕션 등)에서 다양한 설정을 사용할 수 있게 해주는 기능입니다. 예를 들어, 개발 환경에서는 로컬 데이터베이스를 사용하고, 운영 환경에서는 실제 프로덕션 데이터베이스를 사용하는 것과 같이 환경별로 다른 설정이 필요할 때 유용합니다.

## 2. 프로파일의 장점

- **환경별 설정 분리**: 개발, 테스트, 프로덕션 환경별로 다른 설정을 손쉽게 관리할 수 있습니다.
- **코드 변경 없이 설정 전환**: 코드를 수정하지 않고도 프로파일만 변경하여 다른 환경에서 실행할 수 있습니다.
- **설정 중앙화**: 환경별로 필요한 설정을 명확하게 문서화하고 관리할 수 있습니다.
- **테스트 용이성**: 테스트용 프로파일을 별도로 만들어 실제 환경에 영향 없이 테스트를 수행할 수 있습니다.

## 3. 프로파일 설정 방법

Spring Boot에서는 다양한 방법으로 프로파일을 설정할 수 있습니다.

### 3.1 프로파일별 속성 파일 생성

프로파일별로 다른 설정 파일을 만들 수 있습니다:

- `application.yml` 또는 `application.properties`: 모든 프로파일에 공통으로 적용되는 기본 설정
- `application-{profile}.yml` 또는 `application-{profile}.properties`: 특정 프로파일에만 적용되는 설정

예를 들어:
- `application-dev.yml`: 개발 환경 설정
- `application-test.yml`: 테스트 환경 설정
- `application-prod.yml`: 운영 환경 설정

### 3.2 프로파일 활성화하기

프로파일을 활성화하는 방법은 여러 가지가 있습니다:

#### 3.2.1 application.yml에서 활성화

```yaml
spring:
  profiles:
    active: dev  # dev 프로파일 활성화
```

#### 3.2.2 환경 변수로 활성화

```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev

# Windows
set SPRING_PROFILES_ACTIVE=dev
```

#### 3.2.3 애플리케이션 실행 시 명령줄 인수로 활성화

```bash
# Gradle
./gradlew bootRun --args='--spring.profiles.active=dev'

# JAR 파일로 실행 시
java -jar app.jar --spring.profiles.active=dev
```

#### 3.2.4 Docker 컨테이너에서 환경 변수 설정

```yaml
# docker-compose.yml 예시
version: '3'
services:
  app:
    image: my-spring-app
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

## 4. 실제 프로파일 설정 예시

이 프로젝트에서는 다음과 같이 세 가지 프로파일을 사용합니다:

### 4.1 공통 설정 (application.yml)

```yaml
spring:
  application:
    name: attendly-api
  security:
    jwt:
      secret-key: W69y1eJ8PbhfCHWTrTQrNcSY5yYOVBvTmcSoKVGpMxj0rIrx
      expiration-time: 3600000  # 1시간
      refresh-expiration-time: 2592000000  # 30일
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}  # 기본 프로파일은 dev

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method
    tagsSorter: alpha
```

### 4.2 개발 환경 설정 (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/church_attendly?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        show_sql: true
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# Discord 웹훅 설정
discord:
  webhook:
    url: https://discord.com/api/webhooks/1368899732864503818/2kxY39C_ZOklifqUImZpYYp_5mSpWALlnHB2GUT7bAQjpaOjGM3MU7N1YfRUWlt9B0_Q
    min-level: DEBUG

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.attendly: DEBUG
```

### 4.3 테스트 환경 설정 (application-test.yml)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
        show_sql: true
    open-in-view: false
  flyway:
    enabled: false

# Discord 웹훅 설정
discord:
  webhook:
    url: 
    min-level: INFO

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.attendly: DEBUG
```

### 4.4 운영 환경 설정 (application-prod.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:church_attendly}?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: false
        show_sql: false
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# Discord 웹훅 설정
discord:
  webhook:
    url: ${DISCORD_WEBHOOK_URL:}
    min-level: WARN

logging:
  level:
    org.hibernate.SQL: INFO
    org.hibernate.type.descriptor.sql.BasicBinder: INFO
    com.attendly: INFO
```

## 5. 프로파일 활용 사례

### 5.1 테스트 코드에서의 프로파일 활용

테스트 코드에서는 `@ActiveProfiles` 어노테이션을 사용하여 테스트용 프로파일을 활성화할 수 있습니다:

```java
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ActiveProfiles("test")  // test 프로파일 활성화
public class MyTest {
    @Test
    public void testSomething() {
        // 여기서는 application-test.yml 설정이 적용됩니다
    }
}
```

### 5.2 프로파일 조건부 Bean 정의

특정 프로파일에서만 사용되는 Bean을 정의할 수도 있습니다:

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AppConfig {
    
    @Bean
    @Profile("dev")  // dev 프로파일에서만 활성화
    public DataSource devDataSource() {
        // 개발용 데이터소스 설정
    }
    
    @Bean
    @Profile("prod")  // prod 프로파일에서만 활성화
    public DataSource prodDataSource() {
        // 운영용 데이터소스 설정
    }
}
```

## 6. 프로파일 활용 팁

1. **기본 프로파일 설정**: 항상 기본 프로파일(일반적으로 `dev`)을 설정하여 명시적으로 프로파일을 지정하지 않았을 때도 애플리케이션이 제대로 동작하도록 합니다.

2. **민감한 정보 처리**: 비밀번호, API 키 등의 민감한 정보는 환경 변수나 외부 설정으로 처리하고, 프로파일 설정 파일에는 참조만 포함하는 것이 좋습니다.

3. **프로파일 계층 구조**: 공통 설정은 기본 `application.yml`에 두고, 프로파일별 설정 파일에는 해당 환경에서 달라지는 설정만 포함하는 것이 좋습니다.

4. **프로파일 문서화**: 각 프로파일의 용도와 설정 내용을 명확하게 문서화하여 팀원들이 쉽게 이해할 수 있도록 합니다.

## 7. 요약

Spring Boot 프로파일은 다양한 환경에서 애플리케이션의 설정을 쉽게 전환할 수 있게 해주는 강력한 기능입니다. 개발, 테스트, 운영 환경별로 적절한 설정을 분리하여 관리함으로써 애플리케이션의 유연성과 유지보수성을 크게 향상시킬 수 있습니다. 이 가이드를 통해 Spring Boot 프로파일의 기본 개념과 활용 방법을 이해하고, 프로젝트에 효과적으로 적용할 수 있기를 바랍니다. 