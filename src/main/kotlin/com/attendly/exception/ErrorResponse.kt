package com.attendly.exception

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

/**
 * 에러 응답 데이터 클래스
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val code: String,
    val message: String,
    val path: String? = null,
    val errors: List<FieldError>? = null
) {
    /**
     * 필드 에러 정보를 담는 내부 클래스
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class FieldError(
        val field: String,
        val value: Any?,
        val reason: String
    )
    
    companion object {
        /**
         * ErrorMessage로부터 ErrorResponse 생성
         */
        fun of(errorMessage: ErrorMessage, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = errorMessage.status.value(),
                code = errorMessage.code,
                message = errorMessage.message,
                path = path
            )
        }
        
        /**
         * ErrorMessage와 사용자 정의 메시지로 ErrorResponse 생성
         */
        fun withCustomMessage(errorMessage: ErrorMessage, customMessage: String, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = errorMessage.status.value(),
                code = errorMessage.code,
                message = customMessage,
                path = path
            )
        }
        
        /**
         * AttendlyApiException으로부터 ErrorResponse 생성
         */
        fun of(exception: AttendlyApiException, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = exception.errorMessage.status.value(),
                code = exception.errorMessage.code,
                message = exception.message ?: exception.errorMessage.message,
                path = path
            )
        }
    }
} 