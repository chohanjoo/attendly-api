# Attendly API 예외 처리 가이드

## 목차
1. [개요](#개요)
2. [예외 처리 구조](#예외-처리-구조)
3. [ErrorCode](#errorcode)
4. [AttendlyApiException](#attendlyapiexception)
5. [ErrorResponse](#errorresponse)
6. [GlobalExceptionHandler](#globalexceptionhandler)
7. [예외 처리 사용 예시](#예외-처리-사용-예시)
8. [테스트](#테스트)

## 개요

Attendly API는 일관된 형식의 에러 응답을 제공하기 위해 체계적인 예외 처리 시스템을 갖추고 있습니다. 이 문서는 신입 개발자가 Attendly API의 예외 처리 시스템을 이해하고 올바르게 사용할 수 있도록 돕기 위한 가이드입니다.

## 예외 처리 구조

Attendly API의 예외 처리 시스템은 다음과 같은 주요 컴포넌트로 구성되어 있습니다:

1. **ErrorCode (Enum)**: 시스템 내의 모든 에러 코드를 관리하는 열거형 클래스
2. **AttendlyApiException**: 사용자 정의 예외 클래스
3. **ErrorResponse**: 클라이언트에게 반환되는 에러 응답 객체
4. **GlobalExceptionHandler**: 모든 예외를 일관된 방식으로 처리하는 전역 예외 처리기

이 구조를 통해 클라이언트에게 일관된 형식의 에러 응답을 제공하며, 개발자가 비즈니스 로직에 집중할 수 있도록 돕습니다.

## ErrorCode

`ErrorCode`는 시스템 내의 모든 에러 코드를 관리하는 열거형 클래스입니다. 각 에러 코드는 다음 정보를 포함합니다:

- **HTTP 상태 코드**: 응답 상태 코드 (4xx, 5xx)
- **에러 코드**: 고유한 에러 식별자 (예: "E400", "E1000")
- **기본 에러 메시지**: 사용자에게 표시될 기본 메시지

```kotlin
enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 클라이언트 오류 (4xx)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증이 필요합니다"),
    // ... 기타 에러 코드
}
```

에러 코드는 다음 세 가지 카테고리로 분류됩니다:

1. **클라이언트 오류 (4xx)**: 클라이언트의 잘못된 요청으로 인한 오류
2. **사용자 정의 예외**: 비즈니스 로직에서 발생하는 구체적인 오류
3. **서버 오류 (5xx)**: 서버 내부 오류

### 에러 코드 추가 방법

새로운 비즈니스 로직에 대한 에러 코드가 필요한 경우, `ErrorCode` enum에 추가하시면 됩니다. 사용자 정의 예외는 "E1xxx" 형식의 코드를 사용하며, 다음과 같이 추가할 수 있습니다:

```kotlin
// 사용자 정의 예외 예시
MY_CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "E1006", "커스텀 에러 메시지")
```

## AttendlyApiException

`AttendlyApiException`은 Attendly API의 사용자 정의 예외 클래스로, 비즈니스 로직에서 발생하는 예외를 처리하기 위해 사용됩니다. 이 클래스는 `RuntimeException`을 상속받아 Unchecked Exception으로 설계되었습니다.

```kotlin
class AttendlyApiException : RuntimeException {
    val errorCode: ErrorCode
    
    constructor(errorCode: ErrorCode) : super(errorCode.message) {
        this.errorCode = errorCode
    }
    
    // ... 기타 생성자
}
```

이 예외 클래스는 다음 네 가지 생성자를 제공합니다:

1. **errorCode만 전달**: 기본 에러 메시지를 사용
2. **errorCode와 사용자 정의 메시지**: 상세한 에러 메시지를 제공
3. **errorCode와 원인 예외**: 원인 예외를 포함
4. **errorCode, 사용자 정의 메시지, 원인 예외**: 모든 정보를 제공

### 사용 예시

```kotlin
// 기본 에러 메시지 사용
throw AttendlyApiException(ErrorCode.USER_NOT_FOUND)

// 사용자 정의 메시지 제공
throw AttendlyApiException(ErrorCode.USER_NOT_FOUND, "ID가 ${userId}인 사용자를 찾을 수 없습니다")

// 원인 예외 포함
try {
    // 데이터베이스 조회 등 작업
} catch (e: Exception) {
    throw AttendlyApiException(ErrorCode.INTERNAL_SERVER_ERROR, e)
}
```

## ErrorResponse

`ErrorResponse`는 클라이언트에게 반환되는 에러 응답 객체를 표현하는 데이터 클래스입니다. 이 클래스는 다음 정보를 포함합니다:

```kotlin
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val code: String,
    val message: String,
    val path: String? = null,
    val errors: List<FieldError>? = null
) {
    // ... 내부 클래스 및 메소드
}
```

- **timestamp**: 에러 발생 시간
- **status**: HTTP 상태 코드
- **code**: 고유한 에러 코드 
- **message**: 에러 메시지
- **path**: 에러가 발생한 요청 경로
- **errors**: 유효성 검사 실패 시 필드별 오류 정보 (선택적)

### FieldError

유효성 검사 실패 시 각 필드의 오류 정보를 제공하기 위한 내부 클래스입니다:

```kotlin
data class FieldError(
    val field: String,
    val value: Any?,
    val reason: String
)
```

### 생성 메소드

`ErrorResponse`는 다양한 생성 메소드를 제공합니다:

```kotlin
// ErrorCode로부터 생성
val response = ErrorResponse.of(ErrorCode.BAD_REQUEST)

// ErrorCode와 경로로 생성
val response = ErrorResponse.of(ErrorCode.BAD_REQUEST, "/api/users")

// ErrorCode, 사용자 정의 메시지, 경로로 생성
val response = ErrorResponse.withCustomMessage(ErrorCode.BAD_REQUEST, "잘못된 요청입니다", "/api/users")
```

## GlobalExceptionHandler

`GlobalExceptionHandler`는 Spring의 `@RestControllerAdvice`를 사용한 전역 예외 처리기로, 애플리케이션에서 발생하는 모든 예외를 일관된 형식으로 처리합니다.

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    // ... 다양한 예외 처리 메소드
}
```

### 처리되는 예외 유형

1. **AttendlyApiException**: 사용자 정의 예외
2. **ResourceNotFoundException**: 리소스를 찾을 수 없는 예외
3. **AccessDeniedException**: 접근 권한 없음 예외
4. **MethodArgumentNotValidException**: 유효성 검사 실패 예외
5. **BindException**: 바인딩 예외
6. **MethodArgumentTypeMismatchException**: 타입 불일치 예외
7. **NoHandlerFoundException**: 핸들러 없음 예외
8. **Exception**: 처리되지 않은 모든 예외

각 예외는 대응하는 `ErrorCode`와 함께 적절한 `ErrorResponse`로 변환되어 클라이언트에게 반환됩니다.

### 로깅

`GlobalExceptionHandler`는 모든 예외를 로깅하여 디버깅을 용이하게 합니다:

```kotlin
log.error("AttendlyApiException: {}", e.message)
```

## 예외 처리 사용 예시

### 서비스 레이어에서의 예외 발생

```kotlin
@Service
class UserService(private val userRepository: UserRepository) {

    fun getUserById(userId: Long): User {
        return userRepository.findById(userId).orElseThrow {
            AttendlyApiException(ErrorCode.USER_NOT_FOUND, "ID가 ${userId}인 사용자를 찾을 수 없습니다")
        }
    }
    
    fun createUser(userDto: UserDto) {
        if (userRepository.existsByEmail(userDto.email)) {
            throw AttendlyApiException(ErrorCode.DUPLICATE_RESOURCE, "이미 등록된 이메일입니다: ${userDto.email}")
        }
        
        // 사용자 생성 로직
    }
}
```

### API 응답 예시

#### 사용자를 찾을 수 없는 경우

```json
{
  "timestamp": "2023-06-15T14:30:45.123",
  "status": 404,
  "code": "E1002",
  "message": "ID가 123인 사용자를 찾을 수 없습니다",
  "path": "/api/users/123"
}
```

#### 유효성 검사 실패 시

```json
{
  "timestamp": "2023-06-15T14:35:12.456",
  "status": 400,
  "code": "E1000",
  "message": "입력값 검증에 실패했습니다",
  "path": "/api/users",
  "errors": [
    {
      "field": "email",
      "value": "invalid-email",
      "reason": "유효한 이메일 형식이 아닙니다"
    },
    {
      "field": "password",
      "value": "123",
      "reason": "비밀번호는 8자 이상이어야 합니다"
    }
  ]
}
```

## 테스트

테스트 코드를 통해 예외 처리 시스템이 올바르게 작동하는지 확인합니다:

1. **AttendlyApiExceptionTest**: `AttendlyApiException` 클래스의 생성자와 동작을 테스트
2. **GlobalExceptionHandlerTest**: 다양한 예외가 적절한 응답으로 변환되는지 테스트
3. **Integration Test**: API 엔드포인트에서 예외 처리가 올바르게 작동하는지 테스트

### 테스트 실행 방법

```bash
./gradlew test --tests "com.attendly.exception.*"
```

## 모범 사례

1. **구체적인 에러 코드 사용**: 최대한 구체적인 에러 코드를 사용하여 클라이언트가 문제를 쉽게 파악할 수 있도록 합니다.
2. **사용자 친화적인 메시지**: 최종 사용자에게 표시될 수 있는 메시지는 사용자 친화적으로 작성합니다.
3. **디버깅 정보 포함**: 개발자가 디버깅하기 쉽도록 충분한 정보를 포함합니다.
4. **보안 고려**: 민감한 정보는 에러 메시지에 포함하지 않습니다.
5. **테스트 철저히**: 모든 예외 처리 로직은 철저하게 테스트합니다.

이 문서가 Attendly API의 예외 처리 시스템을 이해하고 효과적으로 사용하는 데 도움이 되기를 바랍니다. 추가 질문이나 개선 사항은 개발팀에 문의해 주세요. 