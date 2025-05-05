package com.attendly.exception

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

/**
 * 전역 예외 처리기
 * 애플리케이션에서 발생하는 모든 예외를 일관된 형식으로 처리
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val log = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Attendly API 예외 처리
     */
    @ExceptionHandler(AttendlyApiException::class)
    fun handleAttendalyApiException(
        e: AttendlyApiException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("AttendlyApiException: {}", e)
        val errorResponse = ErrorResponse(
            status = e.errorCode.status.value(),
            code = e.errorCode.code,
            message = e.errorCode.message,
            path = request.requestURI
        )
        return ResponseEntity.status(e.errorCode.status).body(errorResponse)
    }
    
    /**
     * 리소스를 찾을 수 없는 예외 처리
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        e: ResourceNotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("ResourceNotFoundException: {}", e)
        val errorResponse = ErrorResponse(
            status = ErrorCode.RESOURCE_NOT_FOUND.status.value(),
            code = ErrorCode.RESOURCE_NOT_FOUND.code,
            message = e.message ?: ErrorCode.RESOURCE_NOT_FOUND.message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
    
    /**
     * 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        e: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("AccessDeniedException: {}", e)
        val errorResponse = ErrorResponse(
            status = ErrorCode.FORBIDDEN.status.value(),
            code = ErrorCode.FORBIDDEN.code,
            message = e.message ?: ErrorCode.FORBIDDEN.message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }
    
    /**
     * 유효성 검사 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("MethodArgumentNotValidException: {}", e)
        val fieldErrors = e.bindingResult.fieldErrors.map { fieldError ->
            ErrorResponse.FieldError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                reason = fieldError.defaultMessage ?: "유효하지 않은 값입니다"
            )
        }
        
        val errorResponse = ErrorResponse(
            status = ErrorCode.INVALID_INPUT.status.value(),
            code = ErrorCode.INVALID_INPUT.code,
            message = "입력값 검증에 실패했습니다",
            path = request.requestURI,
            errors = fieldErrors
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * 바인딩 예외 처리
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        e: BindException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("BindException: {}", e)
        val fieldErrors = e.bindingResult.fieldErrors.map { fieldError ->
            ErrorResponse.FieldError(
                field = fieldError.field,
                value = fieldError.rejectedValue,
                reason = fieldError.defaultMessage ?: "유효하지 않은 값입니다"
            )
        }
        
        val errorResponse = ErrorResponse(
            status = ErrorCode.INVALID_INPUT.status.value(),
            code = ErrorCode.INVALID_INPUT.code,
            message = "입력값 검증에 실패했습니다",
            path = request.requestURI,
            errors = fieldErrors
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * 타입 불일치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("MethodArgumentTypeMismatchException: {}", e)
        val errorResponse = ErrorResponse(
            status = ErrorCode.BAD_REQUEST.status.value(),
            code = ErrorCode.BAD_REQUEST.code,
            message = "잘못된 타입입니다: ${e.name} - ${e.value}",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
    
    /**
     * 요청한 리소스를 찾을 수 없는 예외 처리
     */
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(
        e: NoHandlerFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("NoHandlerFoundException: {}", e)
        val errorResponse = ErrorResponse(
            status = ErrorCode.RESOURCE_NOT_FOUND.status.value(),
            code = ErrorCode.RESOURCE_NOT_FOUND.code,
            message = "요청한 리소스를 찾을 수 없습니다: ${e.requestURL}",
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
    
    /**
     * 처리되지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unhandled Exception", e)
        val errorResponse = ErrorResponse(
            status = ErrorCode.INTERNAL_SERVER_ERROR.status.value(),
            code = ErrorCode.INTERNAL_SERVER_ERROR.code,
            message = ErrorCode.INTERNAL_SERVER_ERROR.message,
            path = request.requestURI
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
} 