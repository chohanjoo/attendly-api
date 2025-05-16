package com.attendly.config

import com.attendly.api.interceptor.ApiLogInterceptor
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

@Configuration
class WebConfig(
    private val apiLogInterceptor: ApiLogInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(apiLogInterceptor)
            .addPathPatterns("/api/**")
    }

    @Bean
    fun contentCachingFilterRegistration(): FilterRegistrationBean<ContentCachingFilter> {
        val registration = FilterRegistrationBean<ContentCachingFilter>()
        registration.filter = ContentCachingFilter()
        registration.addUrlPatterns("/api/**")
        registration.setName("contentCachingFilter")
        registration.order = 1
        return registration
    }
}

class ContentCachingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        filterChain.doFilter(wrappedRequest, wrappedResponse)
        
        // 응답 내용을 복사해야 함 (필터 사용 후)
        wrappedResponse.copyBodyToResponse()
    }
} 