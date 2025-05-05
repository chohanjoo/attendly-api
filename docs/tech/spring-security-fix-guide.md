# Spring Security 권한 문제 해결 가이드

## 흔히 발생하는 403 Forbidden 오류와 해결 방법

Spring Security를 사용하면서 가장 자주 마주치는 문제 중 하나는 403 Forbidden 오류입니다. 이 문서에서는 이러한 권한 관련 오류의 주요 원인과 해결 방법을 알아보겠습니다.

## 1. 403 오류의 주요 원인

403 Forbidden 오류는 인증(Authentication)은 성공했지만 인가(Authorization)가 실패했을 때 발생합니다. 즉, 사용자는 로그인했지만 요청한 리소스에 접근할 권한이 없는 경우입니다.

주요 원인:

1. **권한 설정 불일치**: `hasRole`과 `hasAuthority` 사용에 있어 불일치
2. **역할 접두사 문제**: 'ROLE_' 접두사 처리 오류
3. **권한 설정 누락**: 사용자에게 필요한 권한이 없음
4. **SpEL 표현식 오류**: PreAuthorize 어노테이션의 SpEL 표현식 오류
5. **설정 잘못됨**: Spring Security 구성 오류

## 2. 가장 흔한 문제: hasRole과 hasAuthority 혼동

### 문제 상황

```kotlin
// 컨트롤러에서:
@PreAuthorize("hasAuthority('ADMIN')")
@GetMapping("/admin/dashboard")
fun adminDashboard() { ... }

// UserDetailsAdapter에서:
override fun getAuthorities(): Collection<GrantedAuthority> {
    return listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
}
```

### 왜 이 문제가 발생하는가?

- `hasAuthority('ADMIN')`는 정확히 "ADMIN"이라는 문자열과 일치하는 권한을 찾습니다.
- 그러나 UserDetailsAdapter에서는 "ROLE_ADMIN" 권한을 부여했습니다.
- 두 문자열이 일치하지 않아 권한 검사가 실패합니다.

### 해결 방법

방법 1: 컨트롤러에서 hasRole 사용 (권장)
```kotlin
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/dashboard")
fun adminDashboard() { ... }
```

방법 2: 컨트롤러에서 정확한 권한 문자열 지정
```kotlin
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@GetMapping("/admin/dashboard")
fun adminDashboard() { ... }
```

방법 3: UserDetailsAdapter 수정
```kotlin
override fun getAuthorities(): Collection<GrantedAuthority> {
    return listOf(
        SimpleGrantedAuthority("ROLE_ADMIN"),  // 역할
        SimpleGrantedAuthority("ADMIN")        // 추가 권한
    )
}
```

## 3. 문제 진단 방법

### 로그 확인하기

Spring Security는 디버그 모드에서 권한 검사 과정에 대한 로그를 남깁니다. 다음과 같이 로깅 레벨을 설정하세요:

```yaml
# application.yml 또는 application.properties
logging:
  level:
    org.springframework.security: DEBUG
```

### 현재 인증 정보 확인하기

현재 사용자의 인증 정보와 권한을 확인하는 디버깅 엔드포인트를 추가해보세요:

```kotlin
@RestController
@RequestMapping("/debug")
class DebugController {

    @GetMapping("/auth-info")
    fun getAuthInfo(authentication: Authentication): Map<String, Any> {
        val authorities = authentication.authorities.map { it.authority }
        val principal = when (val p = authentication.principal) {
            is UserDetails -> mapOf(
                "username" to p.username,
                "authorities" to p.authorities.map { it.authority }
            )
            else -> p.toString()
        }
        
        return mapOf(
            "principal" to principal,
            "authorities" to authorities,
            "authenticated" to authentication.isAuthenticated,
            "details" to (authentication.details?.toString() ?: "null")
        )
    }
}
```

## 4. 권한 관련 오류 해결을 위한 체크리스트

1. **권한 문자열 일치 확인**
   - `UserDetailsService`에서 부여한 권한과 `@PreAuthorize` 표현식이 일치하는지 확인
   - `hasRole`/`hasAuthority` 사용 방식 확인

2. **ROLE_ 접두사 확인**
   - `hasRole`은 내부적으로 'ROLE_' 접두사를 추가
   - `hasAuthority`는 정확한 문자열 일치 확인

3. **SpEL 표현식 확인**
   - 복잡한 SpEL 표현식의 괄호, 연산자 확인
   - 메서드 인자 참조(#id 등)가 올바른지 확인

4. **구성 확인**
   - `@EnableMethodSecurity` 또는 `@EnableGlobalMethodSecurity` 설정 확인
   - 모든 Security 필터가 올바른 순서로 적용되었는지 확인

5. **요청 경로 확인**
   - URL 패턴이 SecurityFilterChain의 구성과 일치하는지 확인
   - Controller Mapping이 올바른지 확인

## 5. 실제 사례: AdminLogController 403 오류

### 문제 상황

관리자 API 호출 시 403 오류 발생:

```
curl -X 'GET' \
  'http://localhost:8080/api/admin/logs/api-calls?page=0&size=20&sort=timestamp%2Cdesc' \
  -H 'Authorization: Bearer eyJhbG...'
```

```kotlin
// AdminLogController.kt
@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasAuthority('ADMIN')")  // 여기가 문제!
class AdminLogController(
    private val systemLogService: SystemLogService
) {
    // ...
}

// UserDetailsAdapter.kt
override fun getAuthorities(): Collection<GrantedAuthority> {
    return listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
}
```

### 원인 분석

1. UserDetailsAdapter는 `"ROLE_ADMIN"` 형태로 권한을 생성
2. AdminLogController는 `hasAuthority('ADMIN')`으로 정확히 "ADMIN" 권한을 요구
3. 권한 문자열 불일치로 인해 403 오류 발생

### 해결 방법

AdminLogController를 수정하여 다른 어드민 컨트롤러와 일관되게 유지:

```kotlin
@RestController
@RequestMapping("/api/admin/logs")
@PreAuthorize("hasRole('ADMIN')")  // hasAuthority('ADMIN') -> hasRole('ADMIN')
class AdminLogController(
    private val systemLogService: SystemLogService
) {
    // ...
}
```

## 6. 권한 관리 모범 사례

1. **일관성 유지**
   - 역할은 항상 `hasRole`로 확인
   - 특수 권한은 항상 `hasAuthority`로 확인
   - 역할과 권한의 명명 규칙 통일

2. **명확한 권한 구조**
   - 역할과 권한을 명확히 구분
   - 개발 초기에 권한 아키텍처 설계

3. **최소 권한 원칙**
   - 필요한 최소한의 권한만 부여
   - 과도한 권한은 보안 위험

4. **권한 검사 추상화**
   - 복잡한 권한 로직은 별도 클래스로 추출
   - `@PreAuthorize("@securityExpressions.canAccess(#id)")`와 같은 방식 사용

## 요약

Spring Security에서 403 Forbidden 오류는 대부분 권한 문자열 불일치나 'ROLE_' 접두사 처리 오류에서 비롯됩니다. `hasRole`과 `hasAuthority`의 차이점을 이해하고, 일관된 방식으로 권한을 관리하면 이러한 문제를 예방할 수 있습니다.

권한 관련 오류가 발생했을 때는 로그를 확인하고, 사용자에게 부여된 실제 권한과 필요한 권한을 비교해보는 것이 중요합니다. 