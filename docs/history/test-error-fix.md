# 테스트 에러 해결 과정

## 1. 에러 상황

Spring Boot 테스트 실행 시 다음과 같은 에러가 발생했습니다:

```
ApplicationTests > contextLoads() FAILED
    java.lang.IllegalStateException
        Caused by: org.springframework.beans.factory.support.BeanDefinitionOverrideException
```

## 2. 에러 원인

1. Bean 정의 충돌
   - 실제 애플리케이션의 `SecurityConfig`와 테스트용 `TestSecurityConfig` 간의 Bean 정의가 충돌
   - 특히 `SecurityFilterChain`, `PasswordEncoder`, `AuthenticationManager` 등의 보안 관련 Bean들이 중복 정의됨

2. 테스트 환경 설정 미비
   - 테스트 환경과 실제 환경의 구분이 명확하지 않음
   - 테스트에 필요한 모의 객체(Mock) 설정이 불완전

## 3. 해결 방법

### 3.1. TestSecurityConfig 수정

```kotlin
@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {

    @Bean
    @Primary
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
        
        return http.build()
    }

    @Bean
    @Primary
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @Primary
    fun authenticationManager(): AuthenticationManager = mockk(relaxed = true)
}
```

주요 변경사항:
- `@Primary` 어노테이션 추가로 Bean 우선순위 지정
- 필요한 보안 관련 Bean들을 명시적으로 정의
- `mockk`를 사용하여 `AuthenticationManager` 모의 객체화

### 3.2. ApplicationTests 수정

```kotlin
@SpringBootTest
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
class ApplicationTests {

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var userDetailsService: UserDetailsService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager

    @Test
    fun contextLoads() {
    }
}
```

주요 변경사항:
- `@ActiveProfiles("test")` 추가로 테스트 프로필 활성화
- 필요한 보안 관련 컴포넌트들을 `@MockkBean`으로 모의 객체화

### 3.3. 테스트 설정 파일 수정 (application.yml)

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
  security:
    jwt:
      secret-key: test-secret-key-for-jwt-token-that-must-be-at-least-256-bits-long
      expiration-time: 3600000
      refresh-expiration-time: 2592000000
```

주요 변경사항:
- `allow-bean-definition-overriding: true` 설정으로 Bean 재정의 허용
- 테스트용 JWT 시크릿 키 설정

## 4. 결과

위의 변경사항들을 적용한 후, 모든 테스트가 성공적으로 실행되었습니다. 이는 다음과 같은 이유로 가능했습니다:

1. Bean 정의 충돌 해결
   - `@Primary` 어노테이션으로 우선순위 지정
   - Bean 재정의 허용 설정 추가

2. 테스트 환경 분리
   - 테스트 프로필 활성화
   - 모의 객체를 통한 의존성 관리

3. 보안 설정 명확화
   - 테스트용 보안 설정 분리
   - JWT 관련 설정 명확화 