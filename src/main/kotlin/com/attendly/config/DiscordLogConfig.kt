package com.attendly.config

import com.attendly.util.DiscordLogger
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment

/**
 * Discord 로깅 관련 설정을 담당하는 클래스
 */
@Configuration
class DiscordLogConfig(private val env: Environment) {
    
    /**
     * 애플리케이션 시작 시 Discord 로깅이 정상 작동하는지 테스트합니다.
     * 'test-discord-log' 프로필이 활성화된 경우에만 테스트 로그를 전송합니다.
     */
    @Bean
    @Profile("test-discord-log")
    fun discordLogTester(): CommandLineRunner {
        return CommandLineRunner {
            val activeProfiles = env.activeProfiles.joinToString()
            val applicationName = env.getProperty("spring.application.name", "Attendly API")
            
            DiscordLogger.info("애플리케이션 시작: $applicationName (프로필: $activeProfiles)")
            DiscordLogger.warn("테스트 경고 메시지")
            
            try {
                // 테스트 예외 발생
                throw RuntimeException("테스트 예외")
            } catch (e: Exception) {
                DiscordLogger.error("테스트 에러 메시지", e)
            }
        }
    }
    
    /**
     * 애플리케이션 시작 이벤트를 Discord에 로깅합니다.
     */
    @Bean
    fun discordStartupLogger(): CommandLineRunner {
        return CommandLineRunner {
            val activeProfiles = env.activeProfiles.joinToString()
            val applicationName = env.getProperty("spring.application.name", "Attendly API")
            
            // 중요한 애플리케이션 이벤트만 WARN 레벨 이상으로 로깅하여 Discord에 전송
            DiscordLogger.systemEvent("애플리케이션 시작: $applicationName (프로필: $activeProfiles)")
        }
    }
} 