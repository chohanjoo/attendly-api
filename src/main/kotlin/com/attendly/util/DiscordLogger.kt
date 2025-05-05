package com.attendly.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Discord 로깅을 위한 유틸리티 클래스
 * 중요한 이벤트나 에러를 Discord로 전송하는 간편한 방법을 제공합니다.
 */
object DiscordLogger {
    private val logger: Logger = LoggerFactory.getLogger("DiscordLogger")

    /**
     * 중요 정보를 Discord에 로깅합니다 (INFO 레벨)
     */
    fun info(message: String) {
        logger.info("[Discord] $message")
    }

    /**
     * 경고 메시지를 Discord에 로깅합니다 (WARN 레벨)
     */
    fun warn(message: String) {
        logger.warn("[Discord] $message")
    }

    /**
     * 에러 메시지를 Discord에 로깅합니다 (ERROR 레벨)
     */
    fun error(message: String) {
        logger.error("[Discord] $message")
    }

    /**
     * 에러와 예외 스택트레이스를 Discord에 로깅합니다 (ERROR 레벨)
     */
    fun error(message: String, throwable: Throwable) {
        logger.error("[Discord] $message", throwable)
    }

    /**
     * 보안 관련 이벤트를 로깅합니다 (WARN/ERROR 레벨)
     */
    fun securityEvent(message: String, isError: Boolean = false) {
        val prefix = "[보안이벤트]"
        if (isError) {
            logger.error("$prefix $message")
        } else {
            logger.warn("$prefix $message")
        }
    }

    /**
     * 시스템 중요 이벤트를 로깅합니다 (WARN 레벨)
     */
    fun systemEvent(message: String) {
        logger.warn("[시스템이벤트] $message")
    }

    /**
     * 인증/인가 이벤트를 로깅합니다 (WARN 레벨)
     */
    fun authEvent(message: String, userId: String? = null) {
        val userInfo = userId?.let { "사용자 ID: $it" } ?: "사용자 불명"
        logger.warn("[인증이벤트] $message ($userInfo)")
    }
} 