package com.attendly

import com.attendly.config.VaultDiscordConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(VaultDiscordConfig::class)
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
