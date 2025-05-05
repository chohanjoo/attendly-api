package com.attendly.security

import com.attendly.util.DiscordLogger
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component

/**
 * 사용자 인증 관련 이벤트를 Discord로 로깅하는 예제 클래스
 * 실제 프로젝트에서는 이 코드를 적절한 위치에 적용하시면 됩니다.
 */
@Component
class AuthEventLogger {

    /**
     * 사용자 로그인 성공 시 Discord에 로깅합니다
     */
    @EventListener
    fun onAuthenticationSuccess(event: AuthenticationSuccessEvent) {
        val username = event.authentication.name
        
        // 로그인 성공은 일반적으로 INFO 레벨이므로 Discord로 전송되지 않습니다
        // 필요한 경우 WARN 이상으로 로깅해야 Discord로 전송됩니다
        DiscordLogger.info("사용자 로그인 성공: $username")
        
        // 관리자 로그인은 중요 이벤트이므로 WARN 레벨로 로깅하여 Discord로 전송
        if (event.authentication.authorities.any { it.authority == "ROLE_ADMIN" }) {
            DiscordLogger.authEvent("관리자 로그인", username)
        }
    }
    
    /**
     * 사용자 로그인 실패 시 Discord에 로깅합니다
     */
    @EventListener
    fun onAuthenticationFailure(event: AuthenticationFailureBadCredentialsEvent) {
        val username = event.authentication.name
        
        // 로그인 실패는 보안 이벤트이므로 WARN 레벨로 로깅하여 Discord로 전송
        DiscordLogger.authEvent("로그인 실패 (잘못된 자격 증명)", username)
        
        // 같은 사용자의 연속 실패는 보안 경고로 처리할 수 있습니다
        // 이 부분은 실제 구현 시 실패 횟수를 추적하는 로직이 필요합니다
        // 여기서는 예시로만 작성합니다
        DiscordLogger.securityEvent("사용자 '$username'의 로그인 5회 연속 실패")
    }
} 