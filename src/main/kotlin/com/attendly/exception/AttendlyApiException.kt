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
    
    /**
     * ErrorMessage를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage) : super(errorMessage.message) {
        this.errorCode = errorMessage.code
    }
    
    /**
     * ErrorMessage와 커스텀 메시지를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage, message: String) : super(message) {
        this.errorCode = errorMessage.code
    }
    
    /**
     * ErrorMessage와 cause를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage, cause: Throwable) : super(errorMessage.message, cause) {
        this.errorCode = errorMessage.code
    }
    
    /**
     * ErrorMessage와 ID값을 사용해 메시지를 생성하는 생성자
     * @deprecated ErrorMessageUtils.withId()를 사용하세요
     */
    @Deprecated("ErrorMessageUtils.withId()를 사용하세요", ReplaceWith("AttendlyApiException(errorMessage, ErrorMessageUtils.withId(errorMessage, id))"))
    constructor(errorMessage: ErrorMessage, id: Long) : super(ErrorMessageUtils.withId(errorMessage, id)) {
        this.errorCode = errorMessage.code
    }
} 