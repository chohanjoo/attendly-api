# Spring Security 비밀번호 암호화 가이드

Spring Security의 `PasswordEncoder`를 사용하여 안전하게 비밀번호를 저장하고 검증하는 방법에 대한 가이드입니다.

## 목차

1. [개요](#개요)
2. [BCryptPasswordEncoder](#bcryptpasswordencoder)
3. [암호화 결과 확인하기](#암호화-결과-확인하기)
4. [비밀번호 암호화의 특성](#비밀번호-암호화의-특성)
5. [구현 예제](#구현-예제)
6. [테스트 코드 작성](#테스트-코드-작성)
7. [보안 관련 고려사항](#보안-관련-고려사항)

## 개요

애플리케이션에서 사용자의 비밀번호는 절대 평문(Plain Text)으로 저장되어서는 안 됩니다. 데이터베이스가 침해되거나 접근 권한이 없는 사람이 데이터를 볼 수 있게 되면 사용자의 계정 정보가 노출될 위험이 있습니다.

Spring Security는 이러한 위험을 방지하기 위해 `PasswordEncoder` 인터페이스를 제공하며, 그 구현체인 `BCryptPasswordEncoder`를 통해 안전한 비밀번호 암호화 기능을 제공합니다.

## BCryptPasswordEncoder

`BCryptPasswordEncoder`는 BCrypt 해싱 함수를 사용하여 비밀번호를 암호화합니다. BCrypt는 비밀번호 해싱을 위해 특별히 설계된 알고리즘으로, 다음과 같은 특징이 있습니다:

- **솔트(Salt) 자동 생성**: 같은 비밀번호라도 매번 다른 해시값 생성
- **작업 요소(Cost Factor)**: 해시 생성에 필요한 계산 비용을 조절할 수 있음
- **적응형 해싱**: 컴퓨터 성능이 향상됨에 따라 보안 강도를 높일 수 있음

### 기본 사용법

```kotlin
// PasswordEncoder 인스턴스 생성 (기본 작업 요소: 10)
val passwordEncoder = BCryptPasswordEncoder()

// 비밀번호 암호화
val encodedPassword = passwordEncoder.encode("admin123")

// 비밀번호 검증 (true/false 반환)
val isMatch = passwordEncoder.matches("admin123", encodedPassword)
```

## 암호화 결과 확인하기

"admin123" 비밀번호를 `BCryptPasswordEncoder`로 여러 번 암호화한 결과는 다음과 같습니다:

```
원본 비밀번호: admin123
암호화된 비밀번호 1: $2a$10$mAKXJL22kfEyAcAvdqGEde.RF1srMqtt/ToCJmbOgSdxlueEQfsVW
암호화된 비밀번호 2: $2a$10$rSd/WoV.H5P.RCg904dCqeZqPiViuNaWnL6MMDk61dUagD5okcMvy
암호화된 비밀번호 3: $2a$10$qMqvNzO/yYGl9rJrS6HDbuCxQfALvTSXn3orgOOEPDKejgIRsBRoC
```

### BCrypt 해시 형식 이해하기

BCrypt 암호화 결과는 다음과 같은 형식으로 구성됩니다:

`$2a$10$솔트와해시값`

- `$2a$`: BCrypt 알고리즘 식별자 (2a는 버전)
- `10$`: 작업 요소(Cost Factor) - 값이 높을수록 해시 생성 시간이 길어지고, 보안이 강화됨
- 나머지 부분: 솔트와 해시값이 결합된 형태

## 비밀번호 암호화의 특성

### 1. 같은 비밀번호도 매번 다른 해시값 생성

BCrypt는 내부적으로 랜덤한 솔트를 생성하기 때문에, 같은 비밀번호를 여러 번 암호화해도 매번 다른 해시값이 생성됩니다.

```kotlin
val password = "admin123"
val hash1 = passwordEncoder.encode(password)  // 결과 1
val hash2 = passwordEncoder.encode(password)  // 결과 2 (결과 1과 다름)
```

### 2. 단방향 암호화

BCrypt는 단방향 해시 함수를 사용하므로, 암호화된 비밀번호로부터 원래 비밀번호를 복원할 수 없습니다. 이는 보안 측면에서 매우 중요한 특성입니다.

### 3. 비밀번호 검증

암호화된 해시값으로부터 원래 비밀번호를 복원할 수는 없지만, `matches` 메서드를 사용하여 입력된 비밀번호가 저장된 해시와 일치하는지 검증할 수 있습니다.

```kotlin
// 비밀번호 검증
passwordEncoder.matches("admin123", encodedPassword)  // true
passwordEncoder.matches("wrongpassword", encodedPassword)  // false
```

## 구현 예제

Spring Security를 사용하는 애플리케이션에서 `BCryptPasswordEncoder`를 구현하는 방법입니다.

### 1. Configuration 설정

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
    
    // 기타 설정...
}
```

### 2. 서비스에서 사용

```kotlin
@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun signup(request: SignupRequest): User {
        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)
        
        // 사용자 생성
        val user = User(
            email = request.email,
            password = encodedPassword,
            // 기타 필드...
        )
        
        return userRepository.save(user)
    }
    
    // 비밀번호 검증 (예: 로그인 처리)
    fun verifyPassword(rawPassword: String, encodedPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, encodedPassword)
    }
}
```

## 테스트 코드 작성

비밀번호 암호화와 관련된 테스트 코드 작성 방법입니다.

```kotlin
@Test
fun `비밀번호 암호화 및 검증 테스트`() {
    val passwordEncoder = BCryptPasswordEncoder()
    val plainPassword = "admin123"
    
    // 비밀번호 암호화
    val encodedPassword = passwordEncoder.encode(plainPassword)
    
    // 검증
    assertTrue(passwordEncoder.matches(plainPassword, encodedPassword))
    assertFalse(passwordEncoder.matches("wrongpassword", encodedPassword))
}

@Test
fun `여러번 암호화해도 서로 다른 해시값이 생성되지만 검증은 성공해야 함`() {
    val passwordEncoder = BCryptPasswordEncoder()
    val plainPassword = "admin123"
    
    // 여러 번 암호화
    val encodedPassword1 = passwordEncoder.encode(plainPassword)
    val encodedPassword2 = passwordEncoder.encode(plainPassword)
    val encodedPassword3 = passwordEncoder.encode(plainPassword)
    
    // 각 해시값은 서로 달라야 함
    assertNotEquals(encodedPassword1, encodedPassword2)
    assertNotEquals(encodedPassword2, encodedPassword3)
    assertNotEquals(encodedPassword1, encodedPassword3)
    
    // 그러나 검증은 모두 성공해야 함
    assertTrue(passwordEncoder.matches(plainPassword, encodedPassword1))
    assertTrue(passwordEncoder.matches(plainPassword, encodedPassword2))
    assertTrue(passwordEncoder.matches(plainPassword, encodedPassword3))
}
```

## 보안 관련 고려사항

### 1. 작업 요소(Cost Factor) 선택

`BCryptPasswordEncoder`의 작업 요소는 해시 생성에 필요한 계산 비용을 결정합니다. 기본값은 10이지만, 더 높은 값으로 설정할 수 있습니다.

```kotlin
// 작업 요소를 12로 설정 (비밀번호 해싱에 더 많은 시간이 소요됨)
val passwordEncoder = BCryptPasswordEncoder(12)
```

작업 요소가 높을수록 해시 생성에 더 많은 시간이 소요되지만, 무차별 대입 공격에 대한 저항성이 높아집니다. 서버 성능과 보안 요구사항을 고려하여 적절한 값을 선택해야 합니다.

### 2. 사용자 로그인 시 타이밍 공격 방지

로그인 처리 시, 아이디가 존재하지 않는 경우와 비밀번호가 틀린 경우에 동일한 오류 메시지를 반환하는 것이 좋습니다. 서로 다른 오류 메시지를 반환하면 공격자가 유효한 사용자 아이디를 파악할 수 있습니다.

### 3. 비밀번호 업데이트 정책

보안 요구사항에 따라 사용자에게 정기적인 비밀번호 변경을 요구하는 정책을 구현할 수 있습니다. 또한, 과거에 사용했던 비밀번호의 재사용을 방지하는 정책도 고려할 수 있습니다.

### 4. 보안 취약점 발견 시 대응

BCrypt 알고리즘에 취약점이 발견되거나 새로운 보안 표준이 등장할 경우, Spring Security는 새로운 `PasswordEncoder` 구현체를 제공할 수 있습니다. 이 경우 `DelegatingPasswordEncoder`를 사용하여 기존 해시와의 호환성을 유지하면서 새로운 알고리즘으로 전환할 수 있습니다. 