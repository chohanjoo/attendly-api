package com.attendly.exception

import com.attendly.exception.ErrorCode
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
         * ErrorCode로부터 ErrorResponse 생성
         */
        fun of(errorCode: ErrorCode, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = errorCode.status.value(),
                code = errorCode.code,
                message = errorCode.message,
                path = path
            )
        }
        
        /**
         * ErrorCode와 사용자 정의 메시지로 ErrorResponse 생성
         */
        fun of(errorCode: ErrorCode, message: String, path: String? = null): ErrorResponse {
            return ErrorResponse(
                status = errorCode.status.value(),
                code = errorCode.code,
                message = message,
                path = path
            )
        }
    }
} 