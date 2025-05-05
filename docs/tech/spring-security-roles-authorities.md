# Spring Security에서 hasRole과 hasAuthority의 차이점

## 개요

Spring Security는 애플리케이션의 보안을 관리하는 강력한 프레임워크입니다. 사용자 권한을 확인하는 방법으로 주로 `hasRole`과 `hasAuthority` 두 가지 메서드를 사용합니다. 이 문서에서는 두 메서드의 차이점과 각각 언제 사용하는 것이 적합한지 알아보겠습니다.

## 기본 개념

### Authority(권한)와 Role(역할)의 의미

Spring Security에서:

- **Authority(권한)**: 특정 작업을 수행할 수 있는 권한을 의미합니다. 일반적으로 세분화된 권한을 나타낼 때 사용합니다.
- **Role(역할)**: 관련 권한들의 집합을 의미합니다. 일반적으로 사용자의 직무나 위치를 나타냅니다.

## hasRole과 hasAuthority의 차이점

### 1. 접두사(prefix) 처리

가장 중요한 차이점은 `ROLE_` 접두사의 처리 방식입니다:

- **hasRole("ADMIN")**: Spring Security는 내부적으로 `"ROLE_"` 접두사를 자동으로 추가하여 `"ROLE_ADMIN"`을 찾습니다.
- **hasAuthority("ADMIN")**: 정확히 "ADMIN"이라는 문자열과 일치하는 권한을 찾습니다. 접두사를 자동으로 추가하지 않습니다.

```kotlin
// 다음 두 표현식은 동일합니다
@PreAuthorize("hasRole('ADMIN')")  // "ROLE_ADMIN" 권한을 찾음
@PreAuthorize("hasAuthority('ROLE_ADMIN')")  // 정확히 "ROLE_ADMIN" 권한을 찾음
```

### 2. 사용 목적

- **hasRole**: 사용자의 역할(Role)을 확인할 때 사용합니다. 예: 관리자, 사용자, 모더레이터 등
- **hasAuthority**: 특정 권한이나 기능에 대한 접근 권한을 확인할 때 사용합니다. 예: 읽기, 쓰기, 삭제 등의 세부 권한

## UserDetails 구현 예시

Spring Security에서 사용자 정보를 관리하는 `UserDetails` 인터페이스 구현 시 권한을 설정하는 방법:

```kotlin
// UserDetailsAdapter 예시
class UserDetailsAdapter(private val user: User) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        // Role 기반 권한: "ROLE_" 접두사 사용
        val roleAuthority = SimpleGrantedAuthority("ROLE_${user.role.name}")
        
        // 추가 권한을 넣을 수도 있음: 접두사 없음
        val permissions = user.permissions.map { 
            SimpleGrantedAuthority(it.name) 
        }
        
        return listOf(roleAuthority) + permissions
    }
    
    // 기타 UserDetails 메서드 구현
    // ...
}
```

## 권한 확인 방법 비교

### 컨트롤러에서 권한 확인

```kotlin
// Role 확인: Spring이 자동으로 'ROLE_' 접두사 추가
@PreAuthorize("hasRole('ADMIN')")
fun adminOnlyEndpoint() { ... }

// Authority 확인: 정확한 문자열 일치 확인
@PreAuthorize("hasAuthority('DELETE_USER')")
fun deleteUserEndpoint() { ... }

// 여러 권한 중 하나 이상 확인
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
fun managerEndpoint() { ... }

// 복잡한 조건
@PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and hasAuthority('APPROVE_USERS'))")
fun complexPermissionEndpoint() { ... }
```

### 메서드 내에서 프로그래밍 방식으로 권한 확인

```kotlin
// SecurityContextHolder를 사용한 프로그래밍적 권한 확인
fun someBusinessMethod() {
    val authentication = SecurityContextHolder.getContext().authentication
    
    if (authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
        // 관리자 권한이 있는 경우
    }
    
    if (authentication.authorities.any { it.authority == "DELETE_USER" }) {
        // 삭제 권한이 있는 경우
    }
}
```

## 모범 사례 (Best Practices)

1. **일관성 유지**: 애플리케이션 전체에서 권한 확인 방식을 일관되게 유지하세요.

2. **Role과 Authority 구분하기**:
   - **Role**: 사용자의 "역할"을 나타낼 때 사용 (예: ADMIN, USER, MANAGER)
   - **Authority**: 세부적인 "작업 권한"을 나타낼 때 사용 (예: READ_DATA, WRITE_DATA, DELETE_USER)

3. **명확한 네이밍**: 권한 이름은 의미를 명확하게 표현하고, 이해하기 쉽게 작성하세요.
   ```
   // 좋은 예:
   READ_USER_DATA, CREATE_POST, DELETE_COMMENT
   
   // 피해야 할 예:
   DATA_PERMISSION_1, ACTION_TYPE_B
   ```

4. **최소 권한 원칙**: 각 기능에 필요한 최소 권한만 부여하고 확인하세요.

5. **사용자 정의 표현식 활용**: 복잡한 권한 로직은 `@Bean`으로 등록하여 재사용하세요.
   ```kotlin
   @Component("securityExpressions")
   class SecurityExpressions {
       fun canAccessUserData(userId: Long): Boolean {
           // 복잡한 로직 구현
       }
   }
   
   // 사용 예시
   @PreAuthorize("@securityExpressions.canAccessUserData(#userId)")
   fun getUserData(userId: Long) { ... }
   ```

## 흔한 실수와 문제 해결

### 1. 접두사 불일치 문제

가장 흔한 403 오류 원인 중 하나는 `hasRole`과 `hasAuthority`의 접두사 처리 방식을 혼동하는 것입니다.

```kotlin
// 문제 상황:
// UserDetailsAdapter에서 권한 부여 시:
override fun getAuthorities(): Collection<GrantedAuthority> {
    return listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
}

// 컨트롤러에서:
@PreAuthorize("hasAuthority('ADMIN')") // 실패! "ADMIN"과 "ROLE_ADMIN"이 일치하지 않음
fun adminFunction() { ... }

// 해결법 (두 가지 중 하나):
@PreAuthorize("hasAuthority('ROLE_ADMIN')") // 정확한 문자열 지정
// 또는
@PreAuthorize("hasRole('ADMIN')") // Spring이 자동으로 'ROLE_' 접두사 추가
```

### 2. 역할과 권한 혼합 사용

```kotlin
// 혼란스러운 방식:
override fun getAuthorities(): Collection<GrantedAuthority> {
    val authorities = mutableListOf<GrantedAuthority>()
    authorities.add(SimpleGrantedAuthority("ROLE_USER"))  // 역할
    authorities.add(SimpleGrantedAuthority("READ_DATA"))  // 권한 (접두사 없음)
    authorities.add(SimpleGrantedAuthority("ROLE_READ_DATA"))  // 이름은 권한 같지만 접두사가 있음
    return authorities
}

// 명확한 방식:
override fun getAuthorities(): Collection<GrantedAuthority> {
    val roles = listOf(SimpleGrantedAuthority("ROLE_USER"))
    val permissions = listOf(
        SimpleGrantedAuthority("READ_DATA"),
        SimpleGrantedAuthority("WRITE_DATA")
    )
    return roles + permissions
}
```

## 결론

Spring Security에서 `hasRole`과 `hasAuthority`는 비슷해 보이지만 중요한 차이가 있습니다. `hasRole`은 내부적으로 'ROLE_' 접두사를 추가하고, 사용자의 역할을 확인할 때 적합합니다. `hasAuthority`는 정확한 문자열 일치를 확인하며, 세부적인 권한을 확인할 때 적합합니다.

일관된 접근 방식을 선택하고 애플리케이션 전체에서 그 방식을 유지하는 것이 가장 중요합니다. 권한 구조를 명확하게 설계하고, 명명 규칙을 일관되게 유지하면 권한 관련 오류를 최소화할 수 있습니다. 