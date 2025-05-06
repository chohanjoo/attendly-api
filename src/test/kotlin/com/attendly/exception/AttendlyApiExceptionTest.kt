package com.attendly.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class AttendlyApiExceptionTest {

    @Test
    fun `생성자 - ErrorMessage만 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        
        // when
        val exception = AttendlyApiException(errorMessage)
        
        // then
        assertEquals(errorMessage, exception.errorMessage)
        assertEquals(errorMessage.message, exception.message)
    }
    
    @Test
    fun `생성자 - ErrorMessage와 메시지를 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val message = "테스트 메시지"
        
        // when
        val exception = AttendlyApiException(errorMessage, message)
        
        // then
        assertEquals(errorMessage, exception.errorMessage)
        assertEquals(message, exception.message)
    }
    
    @Test
    fun `생성자 - ErrorMessage와 cause를 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val cause = RuntimeException("원인 예외")
        
        // when
        val exception = AttendlyApiException(errorMessage, cause)
        
        // then
        assertEquals(errorMessage, exception.errorMessage)
        assertEquals(errorMessage.message, exception.message)
        assertEquals(cause, exception.cause)
    }
    
    @Test
    fun `생성자 - ErrorMessage와 메시지와 cause를 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val message = "테스트 메시지"
        val cause = RuntimeException("원인 예외")
        
        // when
        val exception = AttendlyApiException(errorMessage, message, cause)
        
        // then
        assertEquals(errorMessage, exception.errorMessage)
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }
} 