package com.attendly.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import java.util.UUID

/**
 * 모든 HTTP 요청에 대해 requestId를 MDC에 설정하는 필터
 * 
 * 이미 요청 헤더에 X-Request-ID 값이 존재한다면 그 값을 사용하고,
 * 없다면 신규 UUID를 생성하여 MDC에 설정한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : GenericFilterBean() {

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as HttpServletRequest
            val requestId = httpRequest.getHeader(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString()
            MDC.put(REQUEST_ID_MDC_KEY, requestId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY)
        }
    }
} 