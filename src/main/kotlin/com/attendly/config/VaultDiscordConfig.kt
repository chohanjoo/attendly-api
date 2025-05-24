package com.attendly.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.context.annotation.Configuration
import org.springframework.vault.annotation.VaultPropertySource

/**
 * Vault에서 디스코드 웹훅 관련 설정을 로드하는 설정 클래스
 */
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "discord")
@VaultPropertySource(
    value = ["secret/attendly-api"],
    propertyNamePrefix = "discord."
)
data class VaultDiscordConfig(
    var webhook: WebhookConfig = WebhookConfig()
) {
    data class WebhookConfig(
        var url: String? = null
    )
} 