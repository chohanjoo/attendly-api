# Mockito와 MockK 비교

## 개요

테스트 코드를 작성할 때 의존성을 가진 객체를 모의(Mock)하는 것은 필수적입니다. 자바 생태계에서는 오랫동안 Mockito가 표준으로 사용되어 왔지만, 코틀린에서는 MockK가 더 자연스러운 선택입니다. 이 문서에서는 두 라이브러리의 특징과 차이점, 그리고 코틀린에서 MockK를 사용하는 이유에 대해 설명합니다.

## Mockito

Mockito는 자바 생태계에서 가장 널리 사용되는 모킹 라이브러리입니다.

### 장점
- 광범위한 사용자 기반과 많은 학습 자료
- 직관적인 API
- Spring Boot Test와의 자연스러운 통합
- 오랜 기간 검증된 안정성

### 단점
- 코틀린의 특성(null 안전성, 함수형 프로그래밍, 확장 함수 등)을 완전히 활용하기 어려움
- 코틀린의 final 클래스 모킹을 위해 추가 설정 필요 (Mockito-Kotlin이나 MockK로 해결 가능)
- 람다 함수 모킹 지원이 제한적

### 예제 코드 (Mockito)
```kotlin
@Test
fun testWithMockito() {
    // 모의 객체 생성
    val service = mock(UserService::class.java)
    
    // 스텁 설정
    `when`(service.findById(1L)).thenReturn(User(1L, "홍길동"))
    
    // 검증
    assertEquals("홍길동", service.findById(1L).name)
    verify(service).findById(1L)
}
```

## MockK

MockK는 코틀린을 위해 특별히 설계된 모킹 라이브러리로, 코틀린의 다양한 기능을 활용할 수 있도록 지원합니다.

### 장점
- 코틀린 친화적인 DSL(Domain Specific Language) 제공
- 코틀린의 final 클래스 기본 지원
- 코루틴, 확장 함수, 람다 등 코틀린 특화 기능 지원
- 다양한 모킹 방법 제공 (relaxed, relaxUnitFun, 부분 모킹 등)
- 직관적인 검증 문법

### 단점
- 자바 프로젝트에서는 Mockito보다 사용이 복잡할 수 있음
- Mockito에 비해 학습 자료가 적음

### 예제 코드 (MockK)
```kotlin
@Test
fun testWithMockK() {
    // 모의 객체 생성
    val service = mockk<UserService>()
    
    // 스텁 설정
    every { service.findById(1L) } returns User(1L, "홍길동")
    
    // 검증
    assertEquals("홍길동", service.findById(1L).name)
    verify { service.findById(1L) }
}
```

## 코틀린에서 MockK를 사용하는 이유

1. **코틀린 언어 특성 지원**
   - 코틀린은 기본적으로 모든 클래스가 final이지만, MockK는 이러한 final 클래스도 쉽게 모킹 가능
   - 확장 함수, 속성 위임, 람다 표현식 등 코틀린 고유 기능 지원

2. **DSL 기반 문법**
   - 코틀린의 lambda with receiver 패턴을 활용한 가독성 높은 DSL 제공
   - 백틱(`)을 사용하지 않고도 자연스러운 문법으로 사용 가능

3. **다양한 모킹 방법**
   - relaxed 모킹으로 스텁이 필요 없는 메소드 자동 처리
   - 부분 모킹(spyk)으로 일부 메소드만 오버라이드 가능
   - 코루틴 지원으로 suspend 함수 모킹 용이

4. **인라인 함수와 확장 함수 지원**
   - 코틀린의 인라인 함수와 확장 함수를 효과적으로 모킹
   - 함수형 프로그래밍 스타일에 더 적합한 API 제공

## 문법 비교

| 기능 | Mockito | MockK |
|------|---------|-------|
| 모의 객체 생성 | `mock(Class.class)` | `mockk<Class>()` |
| 스텁 설정 | ``when`(obj.method()).thenReturn(value)`` | `every { obj.method() } returns value` |
| 검증 | `verify(obj).method()` | `verify { obj.method() }` |
| 인자 매칭 | `anyInt()`, `eq(value)` | `any()`, `eq(value)` |
| 응답 체이닝 | ``when`(obj.method()).thenReturn(a, b, c)`` | `every { obj.method() } returnsMany listOf(a, b, c)` |
| 예외 발생 | ``when`(obj.method()).thenThrow(Exception())`` | `every { obj.method() } throws Exception()` |

## 현재 프로젝트에서 사용하는 MockK 기능

이 프로젝트에서는 다음과 같은 MockK 기능을 활용하고 있습니다:

1. **기본 모킹 (mockk)**
   ```kotlin
   private lateinit var attendanceService: AttendanceService
   
   @BeforeEach
   fun setUp() {
       attendanceService = mockk(relaxed = true)
   }
   ```

2. **Relaxed 모킹**
   - `relaxed = true` 옵션으로 스텁이 명시적으로 정의되지 않은 메소드 호출에 대해 기본값 반환
   - 모든 메소드를 스텁할 필요 없이 필요한 메소드만 스텁하여 테스트 코드 간소화

3. **행동 정의 (every)**
   ```kotlin
   every { userDetailsAdapter.getUser() } returns adminUser
   every { authentication.principal } returns userDetailsAdapter
   ```

4. **정확한 호출 검증 (verify with exactly)**
   ```kotlin
   verify(exactly = 1) { attendanceService.hasLeaderAccess(gbsId, 4L) }
   ```

5. **인자 일치 검증**
   - 명시적인 인자 값으로 메소드 호출 검증
   ```kotlin
   every { attendanceService.hasLeaderAccess(1L, 4L) } returns true
   ```

6. **반환값 지정 (returns)**
   ```kotlin
   every { adminUser.role } returns Role.ADMIN
   ```

## 결론

Mockito와 MockK는 모두 강력한 모킹 라이브러리이지만, 코틀린 프로젝트에서는 MockK가 언어 특성에 더 잘 맞는 선택입니다. 특히 코틀린의 DSL 지원, final 클래스 모킹 용이성, 그리고 코루틴 같은 코틀린 특화 기능 지원이 큰 장점입니다.

이 프로젝트에서는 MockK의 relaxed 모킹, 명확한 검증 문법, 간결한 스텁 정의 등을 활용하여 테스트 코드의 가독성과 유지보수성을 높였습니다.

## 참고 자료

- [MockK 공식 문서](https://mockk.io/)
- [Mockito 공식 문서](https://site.mockito.org/)
- [Baeldung - Mockito vs MockK](https://www.baeldung.com/kotlin/mockk) 