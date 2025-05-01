# AdminLogControllerTest 에러 수정 기록

## 문제 상황
`AdminLogControllerTest` 실행 시 7개의 테스트 중 2개가 실패하는 문제가 발생했습니다:
1. `ID로 로그 조회 - 성공` 테스트
2. `권한 없는 사용자 접근 테스트`

## 에러 내용

### 에러 1: ID로 로그 조회 테스트 실패
```
java.lang.AssertionError: Status expected:<200> but was:<404>
```
- 예상된 상태 코드는 200(성공)이었으나, 실제로는 404(Not Found) 응답이 반환됨
- 특정 ID(1L)의 로그 조회 API가 로그를 찾지 못하는 문제

### 에러 2: 권한 없는 사용자 접근 테스트 실패
```
java.lang.AssertionError: Status expected:<403> but was:<200>
```
- 예상된 상태 코드는 403(Forbidden)이었으나, 실제로는 200(OK) 응답이 반환됨
- USER 역할의 사용자가 ADMIN 전용 API에 접근했을 때 차단되지 않는 문제

## 원인 분석

### 에러 1의 원인
`AdminLogControllerTest.kt`의 `setup` 메서드에서 모킹 설정 문제:
```kotlin
// 잘못된 설정
every { systemLogService.getLogById(any()) } returns null
```

위 설정으로 인해 어떤 ID 값이 주어져도 항상 null을 반환하여 `404 Not Found`가 발생했습니다.

### 에러 2의 원인
`TestSecurityConfig.kt`의 보안 설정 문제:
```kotlin
// 설정 미비
.authorizeHttpRequests { auth ->
    auth.anyRequest().permitAll()
}
```

테스트용 보안 설정에서 모든 요청을 허용하도록 되어 있어, 관리자 권한 검사가 제대로 수행되지 않았습니다.

## 해결 과정

### 에러 1의 해결
특정 ID에 대한 응답을 명확히 지정하도록 모킹 설정 수정:
```kotlin
// 수정된 설정
every { systemLogService.getLogById(1L) } returns log1
every { systemLogService.getLogById(999L) } returns null
```

- 로그 ID가 1L일 때는 실제 log1 객체를 반환하도록 지정
- 로그 ID가 999L일 때는 null을 반환하도록 지정
- 이로써 `ID로 로그 조회 - 성공` 테스트가 예상대로 동작

### 에러 2의 해결
테스트용 보안 설정에 관리자 권한 검사 추가:
```kotlin
// 수정된 설정
.authorizeHttpRequests { auth ->
    auth.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
    auth.anyRequest().permitAll()
}
```

- `/api/admin/**` 경로에 대해 `ROLE_ADMIN` 권한을 가진 사용자만 접근 가능하도록 설정
- 이로써 `권한 없는 사용자 접근 테스트`가 예상대로 동작

## 교훈
1. **구체적인 모킹 설정**: `any()`와 같은 일반적인 매처를 사용할 때는 주의가 필요합니다. 특히 테스트케이스마다 다른 반환 값이 필요한 경우 구체적인 입력 값에 대한 응답을 명확히 정의해야 합니다.

2. **테스트 보안 설정**: 테스트 환경에서도 실제 애플리케이션과 유사한 보안 설정을 유지해야 합니다. 단순히 모든 요청을 허용하는 것은 보안 관련 로직을 테스트할 수 없게 만듭니다.

3. **세부적인 오류 확인**: 테스트 실패 시 응답 코드와 같은 세부 정보를 확인하면 문제의 원인을 더 빠르게 파악할 수 있습니다.

## 결론
모킹 설정과 보안 구성을 수정함으로써 모든 테스트가 성공적으로 통과하게 되었습니다. 적절한 모킹 설정과 테스트 환경 구성은 신뢰할 수 있는 테스트를 위해 매우 중요합니다. 