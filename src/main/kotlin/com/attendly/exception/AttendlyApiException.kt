package com.attendly.exception

/**
 * Attendly API 예외 클래스
 * 사용자의 잘못된 요청이나 비즈니스 로직 오류 시 발생하는 예외
 */
class AttendlyApiException : RuntimeException {
    val errorMessage: ErrorMessage
    
    /**
     * ErrorMessage를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage) : super(errorMessage.message) {
        this.errorMessage = errorMessage
    }
    
    /**
     * ErrorMessage와 커스텀 메시지를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage, message: String) : super(message) {
        this.errorMessage = errorMessage
    }
    
    /**
     * ErrorMessage와 cause를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage, cause: Throwable) : super(errorMessage.message, cause) {
        this.errorMessage = errorMessage
    }
    
    /**
     * ErrorMessage와 커스텀 메시지, cause를 사용하는 생성자
     */
    constructor(errorMessage: ErrorMessage, message: String, cause: Throwable) : super(message, cause) {
        this.errorMessage = errorMessage
    }
    
    /**
     * @deprecated ErrorMessageUtils를 사용해 메시지를 생성하세요
     */
    @Deprecated("ErrorMessageUtils.withId()를 사용하세요", ReplaceWith("AttendlyApiException(errorMessage, ErrorMessageUtils.withId(errorMessage, id))"))
    constructor(errorMessage: ErrorMessage, id: Long) : super(ErrorMessageUtils.withId(errorMessage, id)) {
        this.errorMessage = errorMessage
    }
} 