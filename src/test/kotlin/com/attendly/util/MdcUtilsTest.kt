package com.attendly.util

import com.attendly.config.RequestIdFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MdcUtilsTest {

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `getOrCreateRequestId는 이미 존재하는 requestId를 반환한다`() {
        // given
        val expectedRequestId = "existing-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, expectedRequestId)

        // when
        val requestId = MdcUtils.getOrCreateRequestId()

        // then
        assertThat(requestId).isEqualTo(expectedRequestId)
    }

    @Test
    fun `getOrCreateRequestId는 requestId가 없으면 새로 생성한다`() {
        // given: MDC에 requestId가 없음

        // when
        val requestId = MdcUtils.getOrCreateRequestId()

        // then
        assertThat(requestId).isNotNull()
        assertThat(requestId).isNotEmpty()
        
        // UUID 형식인지 확인
        try {
            UUID.fromString(requestId)
        } catch (e: Exception) {
            org.junit.jupiter.api.Assertions.fail("생성된 requestId가 유효한 UUID가 아닙니다: $requestId")
        }
        
        // MDC에도 설정됐는지 확인
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isEqualTo(requestId)
    }

    @Test
    fun `setNewRequestId는 항상 새 requestId를 생성하여 설정한다`() {
        // given
        val oldRequestId = "old-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, oldRequestId)

        // when
        val newRequestId = MdcUtils.setNewRequestId()

        // then
        assertThat(newRequestId).isNotEqualTo(oldRequestId)
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isEqualTo(newRequestId)
    }

    @Test
    fun `clearRequestId는 MDC에서 requestId를 제거한다`() {
        // given
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, "some-request-id")
        
        // when
        MdcUtils.clearRequestId()
        
        // then
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNull()
    }

    @Test
    fun `withMdc는 Callable에 MDC 컨텍스트를 전파한다`() {
        // given
        val requestId = "test-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
        
        // ExecutorService를 통해 다른 스레드에서 실행
        val executor = Executors.newSingleThreadExecutor()
        
        // when
        val callable = MdcUtils.withMdc(Callable {
            // 새 스레드에서 MDC에 접근
            MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)
        })
        val result = executor.submit(callable).get()
        
        // then
        assertThat(result).isEqualTo(requestId)
        
        executor.shutdown()
    }

    @Test
    fun `withMdc는 Runnable에 MDC 컨텍스트를 전파한다`() {
        // given
        val requestId = "test-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
        
        var capturedRequestId: String? = null
        
        // ExecutorService를 통해 다른 스레드에서 실행
        val executor = Executors.newSingleThreadExecutor()
        
        // when
        val runnable = MdcUtils.withMdc(Runnable {
            // 새 스레드에서 MDC에 접근
            capturedRequestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)
        })
        executor.submit(runnable).get()
        
        // then
        assertThat(capturedRequestId).isEqualTo(requestId)
        
        executor.shutdown()
    }

    @Test
    fun `withNewRequestId는 코드 블록 실행 동안 새 requestId를 설정하고 이전 상태로 복원한다`() {
        // given
        val originalRequestId = "original-request-id"
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, originalRequestId)
        
        var insideBlockRequestId: String? = null
        
        // when
        MdcUtils.withNewRequestId {
            insideBlockRequestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)
        }
        
        // then
        // 블록 내부에서는 새 requestId가 설정되었는지 확인
        assertThat(insideBlockRequestId).isNotNull()
        assertThat(insideBlockRequestId).isNotEqualTo(originalRequestId)
        
        // 블록 실행 후에는 원래 requestId로 복원되었는지 확인
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isEqualTo(originalRequestId)
    }
} 