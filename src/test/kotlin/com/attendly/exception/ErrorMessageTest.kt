package com.attendly.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus

class ErrorMessageTest {

    @Test
    fun `ErrorMessage의 기본 메시지가 올바르게 설정되었는지 확인`() {
        // given & when
        val message = ErrorMessage.USER_NOT_FOUND
        
        // then
        assertEquals(HttpStatus.NOT_FOUND, message.status)
        assertEquals("E1002", message.code)
        assertEquals("사용자를 찾을 수 없습니다", message.message)
    }
    
    @Test
    fun `fromCode 메서드가 올바르게 작동하는지 확인`() {
        // given
        val code = "E1002"
        
        // when
        val message = ErrorMessage.fromCode(code)
        
        // then
        assertEquals(ErrorMessage.USER_NOT_FOUND, message)
        assertEquals("사용자를 찾을 수 없습니다", message.message)
    }
    
    @Test
    fun `fromStatus 메서드가 올바르게 작동하는지 확인`() {
        // given
        val status = HttpStatus.NOT_FOUND
        
        // when
        val message = ErrorMessage.fromStatus(status)
        
        // then
        assertEquals(ErrorMessage.RESOURCE_NOT_FOUND, message)
        assertEquals("요청한 리소스를 찾을 수 없습니다", message.message)
    }
    
    @Test
    fun `withId 메서드가 ID를 포함한 메시지를 생성하는지 확인`() {
        // given
        val errorMessage = ErrorMessage.USER_NOT_FOUND
        val userId = 123L
        
        // when
        val message = ErrorMessageUtils.withId(errorMessage, userId)
        
        // then
        assertEquals("사용자를 찾을 수 없습니다: ID 123", message)
    }
    
    @Test
    fun `AttendlyApiException에서 ErrorMessage를 사용하는 경우`() {
        // given
        val errorMessage = ErrorMessage.DELEGATOR_NOT_FOUND
        
        // when
        val exception = AttendlyApiException(errorMessage)
        
        // then
        assertEquals(errorMessage, exception.errorMessage)
        assertEquals("위임자를 찾을 수 없습니다", exception.message)
    }
    
    @Test
    fun `ErrorResponse에서 ErrorMessage를 사용하는 경우`() {
        // given
        val errorMessage = ErrorMessage.INVALID_DELEGATION_DATES
        
        // when
        val response = ErrorResponse.of(errorMessage)
        
        // then
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.status)
        assertEquals("E1000", response.code)
        assertEquals("시작일은 종료일보다 이전이거나 같아야 합니다", response.message)
    }
} 