package com.attendly.exception

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ErrorResponseTest {

    @Test
    fun `of - ErrorMessage만 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        
        // when
        val response = ErrorResponse(
            status = errorMessage.status.value(),
            code = errorMessage.code,
            message = errorMessage.message
        )
        
        // then
        assertNotNull(response.timestamp)
        assertEquals(errorMessage.status.value(), response.status)
        assertEquals(errorMessage.code, response.code)
        assertEquals(errorMessage.message, response.message)
        assertNull(response.path)
        assertNull(response.errors)
    }
    
    @Test
    fun `of - ErrorMessage와 path를 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val path = "/api/test"
        
        // when
        val response = ErrorResponse(
            status = errorMessage.status.value(),
            code = errorMessage.code,
            message = errorMessage.message,
            path = path
        )
        
        // then
        assertNotNull(response.timestamp)
        assertEquals(errorMessage.status.value(), response.status)
        assertEquals(errorMessage.code, response.code)
        assertEquals(errorMessage.message, response.message)
        assertEquals(path, response.path)
        assertNull(response.errors)
    }
    
    @Test
    fun `of - ErrorMessage와 사용자 정의 메시지와 path를 전달하는 경우`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val message = "테스트 메시지"
        val path = "/api/test"
        
        // when
        val response = ErrorResponse(
            status = errorMessage.status.value(),
            code = errorMessage.code,
            message = message,
            path = path
        )
        
        // then
        assertNotNull(response.timestamp)
        assertEquals(errorMessage.status.value(), response.status)
        assertEquals(errorMessage.code, response.code)
        assertEquals(message, response.message)
        assertEquals(path, response.path)
        assertNull(response.errors)
    }
    
    @Test
    fun `of - ErrorResponse 팩토리 메서드 테스트`() {
        // given
        val errorMessage = ErrorMessage.BAD_REQUEST
        val path = "/api/test"
        
        // when
        val response = ErrorResponse.of(errorMessage, path)
        
        // then
        assertNotNull(response.timestamp)
        assertEquals(errorMessage.status.value(), response.status)
        assertEquals(errorMessage.code, response.code)
        assertEquals(errorMessage.message, response.message)
        assertEquals(path, response.path)
        assertNull(response.errors)
    }
    
    @Test
    fun `FieldError - 필드 오류 정보 생성`() {
        // given
        val field = "username"
        val value = ""
        val reason = "빈 값일 수 없습니다"
        
        // when
        val fieldError = ErrorResponse.FieldError(field, value, reason)
        
        // then
        assertEquals(field, fieldError.field)
        assertEquals(value, fieldError.value)
        assertEquals(reason, fieldError.reason)
    }
} 