package com.church.attendly.api.interceptor

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.*

@Component
class ApiLogInterceptor(
    private val objectMapper: ObjectMapper
) : HandlerInterceptor {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val requestMap = Collections.synchronizedMap(HashMap<String, Long>())

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val requestId = UUID.randomUUID().toString()
        requestMap[requestId] = System.currentTimeMillis()
        request.setAttribute("requestId", requestId)
        
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        // 아무 작업 없음
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        try {
            val requestId = request.getAttribute("requestId") as String?
            val startTime = requestMap.remove(requestId)
            
            if (startTime != null) {
                val duration = System.currentTimeMillis() - startTime
                
                val requestBody = getRequestBody(request)
                val responseBody = getResponseBody(response)
                val apiPath = request.requestURI
                val method = request.method
                val status = response.status
                
                val additionalInfo = mapOf(
                    "requestId" to requestId,
                    "apiPath" to apiPath,
                    "method" to method,
                    "status" to status,
                    "duration" to duration,
                    "requestHeaders" to getHeadersMap(request),
                    "responseHeaders" to getHeadersMap(response),
                    "requestBody" to requestBody,
                    "responseBody" to responseBody,
                    "queryParams" to request.queryString
                )
                
                // 로그 레벨 결정 (오류 상태인 경우 WARN 또는 ERROR)
                val logLevel = when {
                    status >= 500 -> "ERROR"
                    status >= 400 -> "WARN"
                    else -> "INFO"
                }
                
                val message = "$method $apiPath - $status ($duration ms)"
                
                // 콘솔에만 로그 출력
                when (logLevel) {
                    "ERROR" -> logger.error(message)
                    "WARN" -> logger.warn(message)
                    else -> logger.info(message)
                }
                
                // 상세 정보 디버그 로그로 출력
                if (logger.isDebugEnabled) {
                    logger.debug("API 상세 정보: {}", objectMapper.writeValueAsString(additionalInfo))
                }
            }
        } catch (e: Exception) {
            logger.error("API 로깅 중 오류 발생", e)
        }
    }
    
    private fun getRequestBody(request: HttpServletRequest): String? {
        if (request is ContentCachingRequestWrapper) {
            val content = request.contentAsByteArray
            if (content.isNotEmpty()) {
                return String(content, Charsets.UTF_8)
            }
        }
        return null
    }
    
    private fun getResponseBody(response: HttpServletResponse): String? {
        if (response is ContentCachingResponseWrapper) {
            val content = response.contentAsByteArray
            if (content.isNotEmpty()) {
                return String(content, Charsets.UTF_8)
            }
        }
        return null
    }
    
    private fun getHeadersMap(request: HttpServletRequest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val headerNames = request.headerNames
        
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            // 민감한 헤더는 로깅하지 않음
            if (!isSecurityHeader(headerName)) {
                headers[headerName] = request.getHeader(headerName)
            }
        }
        
        return headers
    }
    
    private fun getHeadersMap(response: HttpServletResponse): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        response.headerNames.forEach { headerName ->
            if (!isSecurityHeader(headerName)) {
                headers[headerName] = response.getHeader(headerName)
            }
        }
        
        return headers
    }
    
    private fun isSecurityHeader(headerName: String): Boolean {
        val securityHeaders = setOf(
            "authorization", "cookie", "set-cookie", "x-auth-token", "x-api-key"
        )
        return securityHeaders.contains(headerName.lowercase())
    }
} 