package com.attendly.api.controller.admin

import com.attendly.util.DiscordLogger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Discord 로깅을 테스트하기 위한 컨트롤러
 * 개발 환경에서만 사용하고 실제 운영 환경에서는 비활성화하는 것이 좋습니다.
 */
@RestController
@RequestMapping("/api/test/logging")
class LogTestController {

    /**
     * 테스트용 INFO 로그 메시지를 생성합니다 (일반적으로 Discord로 전송되지 않음)
     */
    @GetMapping("/info/{message}")
    fun testInfoLog(@PathVariable message: String): ResponseEntity<String> {
        DiscordLogger.info("INFO 테스트: $message")
        return ResponseEntity.ok("INFO 로그가 생성되었습니다. (Discord로 전송되지 않음)")
    }

    /**
     * 테스트용 WARN 로그 메시지를 생성합니다 (Discord로 전송됨)
     */
    @GetMapping("/warn/{message}")
    fun testWarnLog(@PathVariable message: String): ResponseEntity<String> {
        DiscordLogger.warn("WARN 테스트: $message")
        return ResponseEntity.ok("WARN 로그가 생성되어 Discord로 전송되었습니다.")
    }

    /**
     * 테스트용 ERROR 로그 메시지를 생성합니다 (Discord로 전송됨)
     */
    @GetMapping("/error/{message}")
    fun testErrorLog(@PathVariable message: String): ResponseEntity<String> {
        DiscordLogger.error("ERROR 테스트: $message")
        return ResponseEntity.ok("ERROR 로그가 생성되어 Discord로 전송되었습니다.")
    }

    /**
     * 테스트용 예외를 발생시키고 로깅합니다 (Discord로 전송됨)
     */
    @GetMapping("/exception/{message}")
    fun testException(@PathVariable message: String): ResponseEntity<String> {
        try {
            throw RuntimeException("테스트 예외: $message")
        } catch (e: Exception) {
            DiscordLogger.error("예외 발생 테스트", e)
            return ResponseEntity.ok("예외가 발생하여 Discord로 전송되었습니다.")
        }
    }

    /**
     * 테스트용 보안 이벤트 로그를 생성합니다 (Discord로 전송됨)
     */
    @GetMapping("/security/{message}")
    fun testSecurityEvent(@PathVariable message: String): ResponseEntity<String> {
        DiscordLogger.securityEvent("보안 이벤트 테스트: $message")
        return ResponseEntity.ok("보안 이벤트 로그가 생성되어 Discord로 전송되었습니다.")
    }
}