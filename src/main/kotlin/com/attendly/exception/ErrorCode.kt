package com.attendly.exception

import org.springframework.http.HttpStatus

/**
 * 에러 코드를 관리하는 enum 클래스
 * 상태 코드와 에러 코드만 관리, 메시지는 ErrorMessage로 분리
 */
enum class ErrorCode(
    val status: HttpStatus,
    val code: String
) {
    // 클라이언트 오류 (4xx)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E405"),
    CONFLICT(HttpStatus.CONFLICT, "E409"),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "E422"),
    
    // 사용자 정의 예외
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E1000"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E1001"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E1002"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E1003"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "E1004"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E1005"),
    
    // 서버 오류 (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E503");
    
    /**
     * @deprecated 이 메시지 속성은 호환성을 위해 유지되며, 새 코드는 ErrorMessage를 사용해야 합니다.
     */
    @Deprecated("ErrorMessage enum을 사용하세요", ReplaceWith("ErrorMessage.getDefaultMessage(this)"))
    val message: String
        get() = when(this) {
            BAD_REQUEST -> "잘못된 요청입니다"
            UNAUTHORIZED -> "인증이 필요합니다"
            FORBIDDEN -> "접근 권한이 없습니다"
            RESOURCE_NOT_FOUND -> "요청한 리소스를 찾을 수 없습니다"
            METHOD_NOT_ALLOWED -> "허용되지 않은 메소드입니다"
            CONFLICT -> "리소스 충돌이 발생했습니다"
            UNPROCESSABLE_ENTITY -> "요청을 처리할 수 없습니다"
            INVALID_INPUT -> "입력값이 유효하지 않습니다"
            DUPLICATE_RESOURCE -> "이미 존재하는 리소스입니다"
            USER_NOT_FOUND -> "사용자를 찾을 수 없습니다"
            INVALID_CREDENTIALS -> "아이디 또는 비밀번호가 일치하지 않습니다"
            TOKEN_EXPIRED -> "인증 토큰이 만료되었습니다"
            INVALID_TOKEN -> "유효하지 않은 인증 토큰입니다"
            INTERNAL_SERVER_ERROR -> "서버 내부 오류가 발생했습니다"
            SERVICE_UNAVAILABLE -> "서비스를 일시적으로 사용할 수 없습니다"
        }
} 