# SpringMockK 라이브러리

## 개요

SpringMockK는 [Ninja-Squad](https://www.ninja-squad.com/)에서 개발한 라이브러리로, Spring Boot 테스트 환경에서 MockK를 더 쉽게 사용할 수 있도록 해주는 도구입니다. 이 라이브러리는 Spring의 `@MockBean`, `@SpyBean` 어노테이션과 유사한 `@MockkBean`, `@SpykBean` 어노테이션을 제공하여 Kotlin 개발자가 Mockito 대신 MockK를 사용할 수 있게 해줍니다.

## 주요 특징

1. **Kotlin 친화적인 모킹 환경 제공**: MockK는 Kotlin의 특성을 활용할 수 있도록 설계되었으며, SpringMockK는 이러한 MockK를 Spring 테스트 환경에 통합합니다.

2. **간결한 문법**: Mockito의 `when(...).thenReturn(...)` 대신 `every { ... } returns ...` 와 같은 더 직관적인 문법을 사용할 수 있습니다.

3. **Spring 테스트 통합**: Spring의 테스트 환경과 완벽하게 통합되어 `@WebMvcTest`, `@DataJpaTest` 등의 테스트 어노테이션과 함께 사용할 수 있습니다.

4. **코루틴 지원**: MockK는 코루틴을 기본적으로 지원하므로, 비동기 코드 테스트가 용이합니다.

## 프로젝트에 추가된 이유

우리 프로젝트에 SpringMockK 라이브러리가 추가된 이유는 다음과 같습니다:

1. **Kotlin 프로젝트와의 호환성**: 본 프로젝트는 Kotlin으로 작성되었으며, MockK는 Kotlin과 더 잘 맞는 모킹 라이브러리입니다. SpringMockK는 이러한 MockK를 Spring 환경에서 쉽게 사용할 수 있게 해줍니다.

2. **테스트 코드의 가독성 향상**: MockK의 DSL 기반 문법은 테스트 코드의 가독성을 크게 향상시킵니다. 특히 `every { ... } returns ...` 같은 구문은 Kotlin의 람다 표현식과 잘 어울립니다.

3. **코루틴 지원**: 프로젝트에서 코루틴을 사용할 경우, MockK는 `coEvery`, `coVerify` 등의 코루틴 관련 기능을 제공하여 비동기 코드 테스트를 용이하게 합니다.

4. **타입 안전성**: MockK는 Kotlin의 타입 시스템을 최대한 활용하기 때문에, 타입 안전성이 더 높은 테스트 코드를 작성할 수 있습니다.

## 사용 예시

아래는 SpringMockK를 사용한 테스트 코드 예시입니다:

```kotlin
@WebMvcTest(AuthController::class)
@Import(TestSecurityConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userService: UserService

    @Test
    fun `회원가입 성공 테스트`() {
        val signupRequest = SignupRequest(
            email = "test@example.com",
            password = "password123",
            name = "홍길동",
            role = Role.LEADER,
            departmentId = 1L
        )

        val signupResponse = SignupResponse(
            userId = 1L,
            name = "홍길동",
            email = "test@example.com",
            role = "LEADER"
        )

        // MockK 스타일의 모킹
        every { userService.signup(signupRequest) } returns signupResponse

        // 테스트 로직...
    }
}
```

## 의존성 추가 방법

SpringMockK를 프로젝트에 추가하려면 다음과 같이 Gradle 의존성을 추가하면 됩니다:

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}
```

## 참고 자료

- [SpringMockK GitHub 저장소](https://github.com/Ninja-Squad/springmockk)
- [MockK 공식 문서](https://mockk.io/)
- [Ninja-Squad 블로그](https://blog.ninja-squad.com/) 