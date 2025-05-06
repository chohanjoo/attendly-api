package com.attendly.aspect

import com.attendly.util.MdcUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * @Async 또는 @Scheduled 메소드 실행 시 자동으로 requestId를 MDC에 설정하는 Aspect
 * 
 * - 모든 @Async 메소드 실행 시 새로운 requestId를 생성
 * - 모든 @Scheduled 메소드 실행 시 새로운 requestId를 생성
 * - 메소드 실행 후 requestId를 자동으로 제거
 */
@Aspect
@Component
@Order(0) // 다른 AOP 어드바이스보다 먼저 실행되도록 높은 우선순위 지정
class MdcAspect {
    private val logger: Logger = LoggerFactory.getLogger(MdcAspect::class.java)
    
    /**
     * @Async 어노테이션이 붙은 모든 메소드에 requestId 부여
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Async)")
    fun aroundAsyncMethod(joinPoint: ProceedingJoinPoint): Any? {
        return executeWithNewRequestId(joinPoint, "Async")
    }
    
    /**
     * @Scheduled 어노테이션이 붙은 모든 메소드에 requestId 부여
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    fun aroundScheduledMethod(joinPoint: ProceedingJoinPoint): Any? {
        return executeWithNewRequestId(joinPoint, "Scheduled")
    }
    
    /**
     * 새로운 requestId를 생성하여 메소드를 실행하고, 실행 완료 후 제거
     */
    private fun executeWithNewRequestId(joinPoint: ProceedingJoinPoint, type: String): Any? {
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        
        return MdcUtils.withNewRequestId {
            val requestId = MdcUtils.getOrCreateRequestId()
            logger.debug("$type 메소드 실행: {} (requestId: {})", methodName, requestId)
            
            try {
                joinPoint.proceed()
            } catch (e: Throwable) {
                logger.error("$type 메소드 실행 중 오류 발생: {} (requestId: {})", methodName, requestId, e)
                throw e
            } finally {
                logger.debug("$type 메소드 실행 완료: {} (requestId: {})", methodName, requestId)
            }
        }
    }
} 