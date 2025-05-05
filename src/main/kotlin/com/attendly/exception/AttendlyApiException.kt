package com.attendly.exception

/**
 * Attendly API 예외 클래스
 * 사용자의 잘못된 요청이나 비즈니스 로직 오류 시 발생하는 예외
 */
class AttendlyApiException : RuntimeException {
    val errorCode: ErrorCode
    
    constructor(errorCode: ErrorCode) : super(errorCode.message) {
        this.errorCode = errorCode
    }
    
    constructor(errorCode: ErrorCode, message: String) : super(message) {
        this.errorCode = errorCode
    }
    
    constructor(errorCode: ErrorCode, cause: Throwable) : super(errorCode.message, cause) {
        this.errorCode = errorCode
    }
    
    constructor(errorCode: ErrorCode, message: String, cause: Throwable) : super(message, cause) {
        this.errorCode = errorCode
    }
} 