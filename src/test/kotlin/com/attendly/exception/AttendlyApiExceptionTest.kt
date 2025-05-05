package com.attendly.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class AttendlyApiExceptionTest {

    @Test
    fun `생성자 - ErrorCode만 전달하는 경우`() {
        // given
        val errorCode = ErrorCode.BAD_REQUEST
        
        // when
        val exception = AttendlyApiException(errorCode)
        
        // then
        assertEquals(errorCode, exception.errorCode)
        assertEquals(errorCode.message, exception.message)
    }
    
    @Test
    fun `생성자 - ErrorCode와 메시지를 전달하는 경우`() {
        // given
        val errorCode = ErrorCode.BAD_REQUEST
        val message = "테스트 메시지"
        
        // when
        val exception = AttendlyApiException(errorCode, message)
        
        // then
        assertEquals(errorCode, exception.errorCode)
        assertEquals(message, exception.message)
    }
    
    @Test
    fun `생성자 - ErrorCode와 cause를 전달하는 경우`() {
        // given
        val errorCode = ErrorCode.BAD_REQUEST
        val cause = RuntimeException("원인 예외")
        
        // when
        val exception = AttendlyApiException(errorCode, cause)
        
        // then
        assertEquals(errorCode, exception.errorCode)
        assertEquals(errorCode.message, exception.message)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `생성자 - ErrorCode와 메시지와 cause를 전달하는 경우`() {
        // given
        val errorCode = ErrorCode.BAD_REQUEST
        val message = "테스트 메시지"
        val cause = RuntimeException("원인 예외")
        
        // when
        val exception = AttendlyApiException(errorCode, message, cause)
        
        // then
        assertEquals(errorCode, exception.errorCode)
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }
} 