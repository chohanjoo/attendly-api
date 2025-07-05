package com.attendly.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Configuration
import org.springframework.vault.annotation.VaultPropertySource
import jakarta.annotation.PostConstruct

/**
 * Vault에서 디스코드 웹훅 관련 설정을 로드하는 설정 클래스
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "discord")
@VaultPropertySource(value = ["secret/attendly-api"])
data class VaultDiscordConfig(
    var webhook: WebhookConfig = WebhookConfig()
) {
    private val logger = LoggerFactory.getLogger(VaultDiscordConfig::class.java)
    
    @PostConstruct
    fun init() {
        logger.info("VaultDiscordConfig initialized with webhook URL: ${webhook.url?.take(50)}...")
        if (webhook.url.isNullOrBlank()) {
            logger.warn("Discord webhook URL is null or blank after initialization")
        } else {
            logger.info("Discord webhook URL successfully loaded from Vault")
        }
    }
    
    data class WebhookConfig(
        var url: String? = null
    )
} 