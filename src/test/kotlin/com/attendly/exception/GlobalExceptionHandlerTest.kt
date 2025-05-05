package com.attendly.exception

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GlobalExceptionHandlerTest {

    private lateinit var exceptionHandler: GlobalExceptionHandler
    private lateinit var request: HttpServletRequest
    
    @BeforeEach
    fun setUp() {
        exceptionHandler = GlobalExceptionHandler()
        request = mockk(relaxed = true)
        every { request.requestURI } returns "/api/test"
    }
    
    @Test
    fun `handleAttendalyApiException - AttendlyApiException 처리`() {
        // given
        val errorCode = ErrorCode.BAD_REQUEST
        val exception = AttendlyApiException(errorCode)
        
        // when
        val responseEntity = exceptionHandler.handleAttendalyApiException(exception, request)
        
        // then
        assertEquals(errorCode.status, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(errorCode.status.value(), errorResponse.status)
        assertEquals(errorCode.code, errorResponse.code)
        assertEquals("/api/test", errorResponse.path)
        assertEquals(errorCode.message, errorResponse.message)
    }
    
    @Test
    fun `handleResourceNotFoundException - ResourceNotFoundException 처리`() {
        // given
        val message = "리소스를 찾을 수 없습니다"
        val exception = ResourceNotFoundException(message)
        
        // when
        val responseEntity = exceptionHandler.handleResourceNotFoundException(exception, request)
        
        // then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.status)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code, errorResponse.code)
        assertEquals(message, errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
    }
    
    @Test
    fun `handleAccessDeniedException - AccessDeniedException 처리`() {
        // given
        val message = "접근 권한이 없습니다"
        val exception = AccessDeniedException(message)
        
        // when
        val responseEntity = exceptionHandler.handleAccessDeniedException(exception, request)
        
        // then
        assertEquals(HttpStatus.FORBIDDEN, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.FORBIDDEN.value(), errorResponse.status)
        assertEquals(ErrorCode.FORBIDDEN.code, errorResponse.code)
        assertEquals(message, errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
    }
    
    @Test
    fun `handleMethodArgumentNotValidException - MethodArgumentNotValidException 처리`() {
        // given
        val exception = mockk<MethodArgumentNotValidException>(relaxed = true)
        val bindingResult = mockk<BindingResult>(relaxed = true)
        val fieldError = mockk<FieldError>(relaxed = true)
        
        every { exception.bindingResult } returns bindingResult
        every { bindingResult.fieldErrors } returns listOf(fieldError)
        every { fieldError.field } returns "username"
        every { fieldError.rejectedValue } returns ""
        every { fieldError.defaultMessage } returns "사용자 이름은 필수입니다"
        
        // when
        val responseEntity = exceptionHandler.handleMethodArgumentNotValidException(exception, request)
        
        // then
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status)
        assertEquals(ErrorCode.INVALID_INPUT.code, errorResponse.code)
        assertEquals("입력값 검증에 실패했습니다", errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
        
        val errors = errorResponse.errors
        assertNotNull(errors)
        assertEquals(1, errors.size)
        assertEquals("username", errors[0].field)
        assertEquals("", errors[0].value)
        assertEquals("사용자 이름은 필수입니다", errors[0].reason)
    }
    
    @Test
    fun `handleMethodArgumentTypeMismatchException - MethodArgumentTypeMismatchException 처리`() {
        // given
        val exception = mockk<MethodArgumentTypeMismatchException>(relaxed = true)
        every { exception.name } returns "id"
        every { exception.value } returns "abc"
        
        // when
        val responseEntity = exceptionHandler.handleMethodArgumentTypeMismatchException(exception, request)
        
        // then
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.status)
        assertEquals(ErrorCode.BAD_REQUEST.code, errorResponse.code)
        assertEquals("잘못된 타입입니다: id - abc", errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
    }
    
    @Test
    fun `handleNoHandlerFoundException - NoHandlerFoundException 처리`() {
        // given
        val exception = mockk<NoHandlerFoundException>(relaxed = true)
        every { exception.requestURL } returns "/api/not-found"
        
        // when
        val responseEntity = exceptionHandler.handleNoHandlerFoundException(exception, request)
        
        // then
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.status)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code, errorResponse.code)
        assertEquals("요청한 리소스를 찾을 수 없습니다: /api/not-found", errorResponse.message)
        assertEquals("/api/test", errorResponse.path)
    }
    
    @Test
    fun `handleException - 처리되지 않은 예외 처리`() {
        // given
        val exception = RuntimeException("예상치 못한 오류")
        
        // when
        val responseEntity = exceptionHandler.handleException(exception, request)
        
        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.statusCode)
        
        val errorResponse = responseEntity.body
        assertNotNull(errorResponse)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.status)
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.code, errorResponse.code)
        assertEquals("/api/test", errorResponse.path)
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.message, errorResponse.message)
    }
} 