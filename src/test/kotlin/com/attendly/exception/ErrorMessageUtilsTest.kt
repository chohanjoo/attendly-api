package com.attendly.exception

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate

class ErrorMessageUtilsTest {

    @Test
    fun `withId는 ID가 포함된 메시지를 생성해야 한다`() {
        // given
        val errorMessage = ErrorMessage.USER_NOT_FOUND
        val userId = 123L
        
        // when
        val message = ErrorMessageUtils.withId(errorMessage, userId)
        
        // then
        assertEquals("사용자를 찾을 수 없습니다: ID 123", message)
    }
    
    @Test
    fun `withIds는 여러 ID 정보가 포함된 메시지를 생성해야 한다`() {
        // given
        val errorMessage = ErrorMessage.MEMBER_NOT_IN_GBS
        
        // when
        val message = ErrorMessageUtils.withIds(
            errorMessage,
            "memberId" to 123L,
            "gbsId" to 456L
        )
        
        // then
        assertEquals("해당 멤버는 GBS에 속하지 않습니다 (memberId: 123, gbsId: 456)", message)
    }
    
    @Test
    fun `withIdAndDate는 날짜와 ID가 포함된 메시지를 생성해야 한다`() {
        // given
        val errorMessage = ErrorMessage.NO_ACTIVE_LEADER
        val gbsId = 123L
        val date = LocalDate.of(2023, 5, 15)
        
        // when
        val message = ErrorMessageUtils.withIdAndDate(errorMessage, gbsId, date)
        
        // then
        assertEquals("해당 날짜에 활성화된 GBS 리더를 찾을 수 없습니다: ID 123, 날짜 2023-05-15", message)
    }
    
    @Test
    fun `withField는 필드 정보가 포함된 메시지를 생성해야 한다`() {
        // given
        val errorMessage = ErrorMessage.INVALID_INPUT
        val field = "email"
        val value = "test@example.com"
        
        // when
        val message = ErrorMessageUtils.withField(errorMessage, field, value)
        
        // then
        assertEquals("입력값이 유효하지 않습니다: email = test@example.com", message)
    }
    
    @Test
    fun `withParams는 추가 정보가 포함된 메시지를 생성해야 한다`() {
        // given
        val errorMessage = ErrorMessage.INVALID_INPUT
        val params = mapOf(
            "email" to "test@example.com",
            "age" to 17,
            "allowNull" to null
        )
        
        // when
        val message = ErrorMessageUtils.withParams(errorMessage, params)
        
        // then
        assertTrue(message.startsWith("입력값이 유효하지 않습니다 ("))
        assertTrue(message.contains("email: test@example.com"))
        assertTrue(message.contains("age: 17"))
        assertTrue(message.contains("allowNull: null"))
    }
    
    @Test
    fun `withParams는 빈 맵이 주어지면 기본 메시지만 반환해야 한다`() {
        // given
        val errorMessage = ErrorMessage.INVALID_INPUT
        val params = emptyMap<String, Any?>()
        
        // when
        val message = ErrorMessageUtils.withParams(errorMessage, params)
        
        // then
        assertEquals("입력값이 유효하지 않습니다", message)
    }
} 