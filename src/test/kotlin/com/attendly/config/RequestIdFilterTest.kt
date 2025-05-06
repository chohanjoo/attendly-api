package com.attendly.config

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.slf4j.MDC
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse

class RequestIdFilterTest {

    private lateinit var requestIdFilter: RequestIdFilter
    private lateinit var mockRequest: MockHttpServletRequest
    private lateinit var mockResponse: MockHttpServletResponse
    private var capturedRequestId: String? = null

    @BeforeEach
    fun setUp() {
        requestIdFilter = RequestIdFilter()
        mockRequest = MockHttpServletRequest()
        mockResponse = MockHttpServletResponse()
        capturedRequestId = null
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    // 필터체인 실행 중 MDC 값을 캡처하는 커스텀 필터체인
    private inner class CapturingFilterChain : FilterChain {
        override fun doFilter(request: ServletRequest, response: ServletResponse) {
            capturedRequestId = MDC.get("requestId")
        }
    }

    @Test
    fun `헤더에 Request-ID가 있을 때 해당 값을 MDC에 설정한다`() {
        // given
        val expectedRequestId = "test-request-id-123"
        mockRequest.addHeader("X-Request-ID", expectedRequestId)
        val capturingFilterChain = CapturingFilterChain()

        // when
        requestIdFilter.doFilter(mockRequest, mockResponse, capturingFilterChain)

        // then
        assertThat(capturedRequestId).isEqualTo(expectedRequestId)
        // 필터 종료 후 MDC에서 제거되었는지 확인
        assertThat(MDC.get("requestId")).isNull()
    }

    @Test
    fun `헤더에 Request-ID가 없을 때 새 UUID를 생성하여 MDC에 설정한다`() {
        // given: no header
        val capturingFilterChain = CapturingFilterChain()

        // when
        requestIdFilter.doFilter(mockRequest, mockResponse, capturingFilterChain)

        // then
        assertThat(capturedRequestId).isNotNull()
        assertThat(capturedRequestId).isNotEmpty()
        
        // UUID로 파싱 가능한지 확인
        try {
            UUID.fromString(capturedRequestId)
            // 성공적으로 파싱되면 테스트 통과
        } catch (e: Exception) {
            // 파싱 실패 시 테스트 실패
            org.junit.jupiter.api.Assertions.fail("requestId가 유효한 UUID 형식이 아닙니다: $capturedRequestId")
        }
        
        // 필터 종료 후 MDC에서 제거되었는지 확인
        assertThat(MDC.get("requestId")).isNull()
    }

    @Test
    fun `필터 처리 후 MDC에서 requestId가 제거된다`() {
        // given
        mockRequest.addHeader("X-Request-ID", "test-request-id")
        val capturingFilterChain = CapturingFilterChain()

        // when
        requestIdFilter.doFilter(mockRequest, mockResponse, capturingFilterChain)

        // then
        assertThat(capturedRequestId).isEqualTo("test-request-id")
        assertThat(MDC.get("requestId")).isNull()
    }
} 