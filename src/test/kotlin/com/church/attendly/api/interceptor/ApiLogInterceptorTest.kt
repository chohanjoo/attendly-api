package com.church.attendly.api.interceptor

import com.church.attendly.service.SystemLogService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import kotlin.test.assertEquals

class ApiLogInterceptorTest {

    private lateinit var apiLogInterceptor: ApiLogInterceptor
    private lateinit var systemLogService: SystemLogService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mockRequest: MockHttpServletRequest
    private lateinit var mockResponse: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        systemLogService = mockk(relaxed = true)
        objectMapper = ObjectMapper()
        apiLogInterceptor = ApiLogInterceptor(systemLogService, objectMapper)
        mockRequest = MockHttpServletRequest()
        mockResponse = MockHttpServletResponse()
    }

    @Test
    fun `afterCompletion should log API call details`() {
        // Given
        val handler = Any()
        val requestWrapper = ContentCachingRequestWrapper(mockRequest)
        val responseWrapper = ContentCachingResponseWrapper(mockResponse)
        
        // 요청 설정
        mockRequest.method = "POST"
        mockRequest.requestURI = "/api/users"
        mockRequest.contentType = "application/json"
        mockRequest.setContent("""{"name":"홍길동","email":"hong@example.com"}""".toByteArray())
        
        // 응답 설정
        mockResponse.status = 201
        mockResponse.contentType = "application/json"
        responseWrapper.writer.write("""{"id":1,"name":"홍길동","email":"hong@example.com"}""")
        responseWrapper.writer.flush()
        
        // API 호출 시작 시뮬레이션
        apiLogInterceptor.preHandle(requestWrapper, responseWrapper, handler)
        
        // When
        apiLogInterceptor.afterCompletion(requestWrapper, responseWrapper, handler, null)
        
        // Then
        val categorySlot = slot<String>()
        val messageSlot = slot<String>()
        
        verify { 
            systemLogService.createLog(
                eq("INFO"),
                capture(categorySlot),
                capture(messageSlot),
                any(),
                any()
            )
        }
        
        assertEquals("API_CALL", categorySlot.captured)
        assert(messageSlot.captured.contains("POST /api/users"))
        assert(messageSlot.captured.contains("201"))
    }
    
    @Test
    fun `afterCompletion should handle error responses correctly`() {
        // Given
        val handler = Any()
        val requestWrapper = ContentCachingRequestWrapper(mockRequest)
        val responseWrapper = ContentCachingResponseWrapper(mockResponse)
        
        // 요청 설정
        mockRequest.method = "GET"
        mockRequest.requestURI = "/api/users/999"
        
        // 오류 응답 설정
        mockResponse.status = 404
        mockResponse.contentType = "application/json"
        responseWrapper.writer.write("""{"error":"User not found","status":404}""")
        responseWrapper.writer.flush()
        
        // API 호출 시작 시뮬레이션
        apiLogInterceptor.preHandle(requestWrapper, responseWrapper, handler)
        
        // When
        apiLogInterceptor.afterCompletion(requestWrapper, responseWrapper, handler, null)
        
        // Then
        val levelSlot = slot<String>()
        
        verify { 
            systemLogService.createLog(
                capture(levelSlot),
                eq("API_CALL"),
                any(),
                any(),
                any()
            )
        }
        
        assertEquals("WARN", levelSlot.captured)
    }
} 