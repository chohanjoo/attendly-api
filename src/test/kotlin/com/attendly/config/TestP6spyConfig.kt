package com.attendly.config

import com.p6spy.engine.spy.P6SpyOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import jakarta.annotation.PostConstruct

@TestConfiguration
class TestP6spyConfig {
    
    @PostConstruct
    fun setLogMessageFormat() {
        try {
            // 테스트 환경에서는 간소화된 SQL 포맷터 사용
            P6SpyOptions.getActiveInstance().logMessageFormat = TestQuerydslPrettySqlFormatter::class.java.name
        } catch (e: Exception) {
            // 테스트 환경에서 P6SpyOptions를 초기화할 수 없는 경우 무시
        }
    }
    
    @Bean
    @Primary
    fun p6spyConfig(): P6spyConfig {
        // P6SpyOptions.getActiveInstance() 정적 메서드를 모킹
        mockkStatic(P6SpyOptions::class)
        
        // P6SpyOptions.getActiveInstance()가 반환하는 객체를 모킹
        val mockP6SpyOptions = mockk<P6SpyOptions>(relaxed = true)
        every { P6SpyOptions.getActiveInstance() } returns mockP6SpyOptions
        
        // P6spyConfig 반환
        return mockk<P6spyConfig>(relaxed = true)
    }
} 