package com.attendly.util

import com.attendly.config.RequestIdFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.UUID
import java.util.concurrent.Callable

/**
 * MDC (Mapped Diagnostic Context) 관련 유틸리티 클래스
 * 
 * 웹 요청이 아닌 환경(배치, 스케줄러 등)에서 MDC에 requestId를 설정할 수 있도록 도와주는 유틸리티 메서드를 제공
 */
object MdcUtils {
    private val logger: Logger = LoggerFactory.getLogger(MdcUtils::class.java)

    /**
     * 현재 MDC에 설정된 requestId를 반환
     * requestId가 없는 경우 새로 생성하여 MDC에 설정 후 반환
     */
    fun getOrCreateRequestId(): String {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY) ?: UUID.randomUUID().toString().also {
            MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, it)
            logger.trace("새 requestId 생성: {}", it)
        }
    }

    /**
     * 명시적으로 새 requestId를 생성하여 MDC에 설정
     */
    fun setNewRequestId(): String {
        val requestId = UUID.randomUUID().toString()
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
        logger.trace("새 requestId 설정: {}", requestId)
        return requestId
    }

    /**
     * 주어진 requestId를 MDC에 설정
     */
    fun setRequestId(requestId: String) {
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
    }

    /**
     * MDC에서 requestId 제거
     */
    fun clearRequestId() {
        MDC.remove(RequestIdFilter.REQUEST_ID_MDC_KEY)
    }

    /**
     * Runnable에 MDC 컨텍스트를 전파하여 실행
     * 주로 스레드 풀이나 비동기 작업에서 MDC 컨텍스트를 유지하기 위해 사용
     */
    fun <T> withMdc(callable: Callable<T>): Callable<T> {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        return Callable {
            val oldContextMap = MDC.getCopyOfContextMap()
            try {
                MDC.setContextMap(contextMap)
                callable.call()
            } finally {
                if (oldContextMap != null) {
                    MDC.setContextMap(oldContextMap)
                } else {
                    MDC.clear()
                }
            }
        }
    }

    /**
     * Runnable에 MDC 컨텍스트를 전파하여 실행
     * 주로 스레드 풀이나 비동기 작업에서 MDC 컨텍스트를 유지하기 위해 사용
     */
    fun withMdc(runnable: Runnable): Runnable {
        val contextMap = MDC.getCopyOfContextMap() ?: emptyMap()
        return Runnable {
            val oldContextMap = MDC.getCopyOfContextMap()
            try {
                MDC.setContextMap(contextMap)
                runnable.run()
            } finally {
                if (oldContextMap != null) {
                    MDC.setContextMap(oldContextMap)
                } else {
                    MDC.clear()
                }
            }
        }
    }

    /**
     * 새 requestId로 코드 블록을 실행한 후 이전 MDC 상태로 복원
     */
    fun <T> withNewRequestId(block: () -> T): T {
        val oldMdc = MDC.getCopyOfContextMap()
        try {
            setNewRequestId()
            return block()
        } finally {
            if (oldMdc != null) {
                MDC.setContextMap(oldMdc)
            } else {
                MDC.clear()
            }
        }
    }
} 