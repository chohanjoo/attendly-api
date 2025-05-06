package com.attendly.aspect

import com.attendly.config.RequestIdFilter
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.mockk.*
import org.slf4j.MDC
import java.util.UUID

class MdcAspectTest {

    private lateinit var mdcAspect: MdcAspect
    private lateinit var mockJoinPoint: ProceedingJoinPoint
    private lateinit var mockSignature: Signature

    @BeforeEach
    fun setUp() {
        mdcAspect = MdcAspect()
        mockJoinPoint = mockk<ProceedingJoinPoint>()
        mockSignature = mockk<Signature>()
        
        every { mockJoinPoint.signature } returns mockSignature
        every { mockSignature.declaringTypeName } returns "com.example.TestClass"
        every { mockSignature.name } returns "testMethod"
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
        clearAllMocks()
    }

    @Test
    fun `aroundAsyncMethod는 새 requestId를 생성하고 메소드 실행 후 제거한다`() {
        // given
        val returnValue = "test result"
        every { mockJoinPoint.proceed() } returns returnValue
        
        // when
        val result = mdcAspect.aroundAsyncMethod(mockJoinPoint)
        
        // then
        assertThat(result).isEqualTo(returnValue)
        verify(exactly = 1) { mockJoinPoint.proceed() }
        
        // MDC에 requestId가 없는지 확인 (메소드 종료 후 제거됨)
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `aroundScheduledMethod는 새 requestId를 생성하고 메소드 실행 후 제거한다`() {
        // given
        val returnValue = "test result"
        every { mockJoinPoint.proceed() } returns returnValue
        
        // when
        val result = mdcAspect.aroundScheduledMethod(mockJoinPoint)
        
        // then
        assertThat(result).isEqualTo(returnValue)
        verify(exactly = 1) { mockJoinPoint.proceed() }
        
        // MDC에 requestId가 없는지 확인 (메소드 종료 후 제거됨)
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `예외 발생 시에도 MDC에서 requestId를 제거한다`() {
        // given
        MDC.clear() // 명시적으로 MDC 정리
        every { mockJoinPoint.proceed() } throws RuntimeException("Test exception")
        
        // when & then
        try {
            mdcAspect.aroundAsyncMethod(mockJoinPoint)
        } catch (e: RuntimeException) {
            // 예외가 발생해도 MDC는 정리되어야 함
            assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNull()
            MDC.clear() // 테스트 종료 시 명시적으로 MDC 정리
        }
    }

    @Test
    fun `기존 MDC 컨텍스트가 있을 경우 실행 후 복원한다`() {
        // given
        val originalRequestId = "original-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, originalRequestId)
        
        val returnValue = "test result"
        every { mockJoinPoint.proceed() } returns returnValue
        
        // when
        val result = mdcAspect.aroundAsyncMethod(mockJoinPoint)
        
        // then
        assertThat(result).isEqualTo(returnValue)
        
        // 원래 requestId로 복원됐는지 확인
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isEqualTo(originalRequestId)
    }
}