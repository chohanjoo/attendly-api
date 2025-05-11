package com.attendly.api.dto

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiResponseTest {
    
    @Test
    fun `기본 ApiResponse 생성 테스트`() {
        val response = ApiResponse<String>()
        
        assertTrue(response.success)
        assertNotNull(response.timestamp)
        assertNull(response.data)
        assertEquals("SUCCESS", response.message)
        assertEquals(200, response.code)
    }
    
    @Test
    fun `success 팩토리 메서드 테스트`() {
        val testData = "테스트 데이터"
        val response = ApiResponse.success(testData, "요청 성공")
        
        assertTrue(response.success)
        assertNotNull(response.timestamp)
        assertEquals(testData, response.data)
        assertEquals("요청 성공", response.message)
        assertEquals(200, response.code)
    }
    
    @Test
    fun `successNoData 팩토리 메서드 테스트`() {
        val response = ApiResponse.successNoData<String>("데이터 없음")
        
        assertTrue(response.success)
        assertNotNull(response.timestamp)
        assertNull(response.data)
        assertEquals("데이터 없음", response.message)
        assertEquals(200, response.code)
    }
    
    @Test
    fun `error 팩토리 메서드 테스트`() {
        val response = ApiResponse.error<String>("에러 발생", 400)
        
        assertFalse(response.success)
        assertNotNull(response.timestamp)
        assertNull(response.data)
        assertEquals("에러 발생", response.message)
        assertEquals(400, response.code)
    }
    
    @Test
    fun `PageResponse 생성 테스트`() {
        val items = listOf("item1", "item2", "item3")
        val pageResponse = PageResponse(items, 10L, true)
        
        assertEquals(items, pageResponse.items)
        assertEquals(10L, pageResponse.totalCount)
        assertTrue(pageResponse.hasMore)
    }
    
    @Test
    fun `PageResponse 기본값 테스트`() {
        val items = listOf("item1", "item2")
        val pageResponse = PageResponse(items)
        
        assertEquals(items, pageResponse.items)
        assertEquals(2L, pageResponse.totalCount)
        assertFalse(pageResponse.hasMore)
    }
} 