package com.attendly.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 클라이언트 오류 (4xx)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E405", "허용되지 않은 메소드입니다"),
    CONFLICT(HttpStatus.CONFLICT, "E409", "리소스 충돌이 발생했습니다"),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "E422", "요청을 처리할 수 없습니다"),
    
    // 사용자 정의 예외
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E1000", "입력값이 유효하지 않습니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E1001", "이미 존재하는 리소스입니다"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E1002", "사용자를 찾을 수 없습니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E1003", "아이디 또는 비밀번호가 일치하지 않습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "E1004", "인증 토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E1005", "유효하지 않은 인증 토큰입니다"),
    
    // 서버 오류 (5xx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E503", "서비스를 일시적으로 사용할 수 없습니다");
} 