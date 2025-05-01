package com.church.attendly.aspect

import com.church.attendly.service.SystemLogService
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * 시스템 로깅을 위한 AOP 구현체
 */
@Aspect
@Component
class LoggingAspect(
    private val systemLogService: SystemLogService
) {

    /**
     * 중요 컨트롤러 메서드에 대한 포인트컷
     */
    @Pointcut("within(com.church.attendly.api.controller..*) && @annotation(org.springframework.web.bind.annotation.PostMapping)")
    fun postMappingMethods() {}

    @Pointcut("within(com.church.attendly.api.controller..*) && @annotation(org.springframework.web.bind.annotation.PutMapping)")
    fun putMappingMethods() {}

    @Pointcut("within(com.church.attendly.api.controller..*) && @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    fun deleteMappingMethods() {}

    @Pointcut("within(com.church.attendly.api.controller.admin..*)")
    fun adminControllerMethods() {}

    /**
     * 중요 컨트롤러 메서드 실행 후 성공 로깅
     */
    @AfterReturning(
        pointcut = "postMappingMethods() || putMappingMethods() || deleteMappingMethods() || adminControllerMethods()",
        returning = "result"
    )
    fun logAfterControllerMethod(joinPoint: JoinPoint, result: Any?) {
        try {
            val request = getRequest()
            val signature = joinPoint.signature as MethodSignature
            val methodName = signature.method.name
            val className = signature.declaringType.simpleName

            val category = determineCategory(className)
            val message = "성공: $className.$methodName()"
            val additionalInfo = mapOf(
                "method" to methodName,
                "class" to className,
                "args" to joinPoint.args.map { it.toString() },
                "result" to result?.toString()
            )

            systemLogService.createLog(
                level = "INFO",
                category = category,
                message = message,
                additionalInfo = additionalInfo,
                request = request
            )
        } catch (e: Exception) {
            // 로깅 중 오류 발생 시 스택트레이스만 출력하고 메인 흐름에 영향 없도록 함
            e.printStackTrace()
        }
    }

    /**
     * 중요 컨트롤러 메서드 실행 중 예외 발생 시 로깅
     */
    @AfterThrowing(
        pointcut = "postMappingMethods() || putMappingMethods() || deleteMappingMethods() || adminControllerMethods()",
        throwing = "exception"
    )
    fun logAfterThrowing(joinPoint: JoinPoint, exception: Exception) {
        try {
            val request = getRequest()
            val signature = joinPoint.signature as MethodSignature
            val methodName = signature.method.name
            val className = signature.declaringType.simpleName

            val category = determineCategory(className)
            val message = "오류: $className.$methodName() - ${exception.message}"
            val additionalInfo = mapOf(
                "method" to methodName,
                "class" to className,
                "args" to joinPoint.args.map { it.toString() },
                "exception" to mapOf(
                    "type" to exception.javaClass.name,
                    "message" to exception.message,
                    "stackTrace" to exception.stackTraceToString()
                )
            )

            systemLogService.createLog(
                level = "ERROR",
                category = category,
                message = message,
                additionalInfo = additionalInfo,
                request = request
            )
        } catch (e: Exception) {
            // 로깅 중 오류 발생 시 스택트레이스만 출력하고 메인 흐름에 영향 없도록 함
            e.printStackTrace()
        }
    }

    /**
     * 현재 요청 객체 가져오기
     */
    private fun getRequest(): HttpServletRequest? {
        return try {
            val requestAttributes = RequestContextHolder.getRequestAttributes()
            if (requestAttributes is ServletRequestAttributes) {
                requestAttributes.request
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 클래스명으로 로그 카테고리 결정
     */
    private fun determineCategory(className: String): String {
        return when {
            className.contains("Attendance") -> "ATTENDANCE"
            className.contains("User") -> "USER_MANAGEMENT"
            className.contains("Admin") -> "ADMIN"
            className.contains("Security") || className.contains("Auth") -> "SECURITY"
            className.contains("Department") || className.contains("Village") || className.contains("Gbs") -> "ORGANIZATION"
            else -> "APPLICATION"
        }
    }
} 