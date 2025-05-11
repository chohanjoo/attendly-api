package com.attendly.api.util

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.PageResponse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResponseUtilTest {
    
    @Test
    fun `success 메서드 테스트`() {
        val data = "테스트 데이터"
        val response = ResponseUtil.success(data, "성공", HttpStatus.OK)
        
        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
        assertTrue(response.body?.success ?: false)
        assertEquals(data, response.body?.data)
        assertEquals("성공", response.body?.message)
        assertEquals(200, response.body?.code)
    }
    
    @Test
    fun `successNoData 메서드 테스트`() {
        val response = ResponseUtil.successNoData<String>("데이터 없음", HttpStatus.OK)
        
        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
        assertTrue(response.body?.success ?: false)
        assertNull(response.body?.data)
        assertEquals("데이터 없음", response.body?.message)
        assertEquals(200, response.body?.code)
    }
    
    @Test
    fun `successList 메서드 테스트`() {
        val items = listOf("아이템1", "아이템2", "아이템3")
        val response = ResponseUtil.successList(
            items = items,
            totalCount = 10L,
            hasMore = true,
            message = "목록 조회 성공",
            status = HttpStatus.OK
        )
        
        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
        assertTrue(response.body?.success ?: false)
        assertEquals("목록 조회 성공", response.body?.message)
        assertEquals(200, response.body?.code)
        
        val pageResponse = response.body?.data
        assertEquals(items, pageResponse?.items)
        assertEquals(10L, pageResponse?.totalCount)
        assertTrue(pageResponse?.hasMore ?: false)
    }
    
    @Test
    fun `error 메서드 테스트`() {
        val response = ResponseUtil.error<String>(
            message = "오류 발생",
            errorCode = 400,
            status = HttpStatus.BAD_REQUEST
        )
        
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCodeValue)
        assertFalse(response.body?.success ?: true)
        assertNull(response.body?.data)
        assertEquals("오류 발생", response.body?.message)
        assertEquals(400, response.body?.code)
    }
    
    @Test
    fun `created 메서드 테스트`() {
        val data = "생성된 데이터"
        val response = ResponseUtil.created(data, "생성 성공")
        
        assertEquals(HttpStatus.CREATED.value(), response.statusCodeValue)
        assertTrue(response.body?.success ?: false)
        assertEquals(data, response.body?.data)
        assertEquals("생성 성공", response.body?.message)
        assertEquals(200, response.body?.code)
    }
} 