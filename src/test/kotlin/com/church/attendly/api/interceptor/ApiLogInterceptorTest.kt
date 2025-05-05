package com.church.attendly.api.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.contains
import org.slf4j.LoggerFactory
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import kotlin.test.assertTrue

@ExtendWith(OutputCaptureExtension::class)
class ApiLogInterceptorTest {

    private lateinit var apiLogInterceptor: ApiLogInterceptor
    private lateinit var objectMapper: ObjectMapper
    private lateinit var mockRequest: MockHttpServletRequest
    private lateinit var mockResponse: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        apiLogInterceptor = ApiLogInterceptor(objectMapper)
        mockRequest = MockHttpServletRequest()
        mockResponse = MockHttpServletResponse()
    }

    @Test
    fun `afterCompletion should log API call details`(output: CapturedOutput) {
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
        assertTrue(output.toString().contains("POST /api/users - 201"))
    }
    
    @Test
    fun `afterCompletion should handle error responses correctly`(output: CapturedOutput) {
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
        assertTrue(output.toString().contains("GET /api/users/999 - 404"))
    }
} 