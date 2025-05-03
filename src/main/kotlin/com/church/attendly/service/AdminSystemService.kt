package com.church.attendly.service

import com.church.attendly.api.dto.*
import com.church.attendly.domain.entity.SystemSetting
import com.church.attendly.domain.repository.SystemSettingRepository
import com.church.attendly.exception.ResourceNotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDateTime

@Service
class AdminSystemService(
    private val systemSettingRepository: SystemSettingRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 시스템 설정 생성/수정
     */
    @Transactional
    // @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveSystemSetting(request: SystemSettingRequest): SystemSettingResponse {
        val existingSetting = systemSettingRepository.findByKey(request.key)

        val systemSetting = if (existingSetting.isPresent) {
            val setting = existingSetting.get()
            SystemSetting(
                id = setting.id,
                key = request.key,
                value = request.value,
                description = request.description,
                createdAt = setting.createdAt
            )
        } else {
            SystemSetting(
                key = request.key,
                value = request.value,
                description = request.description
            )
        }

        val savedSetting = systemSettingRepository.save(systemSetting)

        return SystemSettingResponse(
            id = savedSetting.id ?: 0L,
            key = savedSetting.key,
            value = savedSetting.value,
            description = savedSetting.description,
            createdAt = savedSetting.createdAt,
            updatedAt = savedSetting.updatedAt
        )
    }

    /**
     * 시스템 설정 삭제
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun deleteSystemSetting(key: String) {
        val setting = systemSettingRepository.findByKey(key)
            .orElseThrow { ResourceNotFoundException("설정을 찾을 수 없습니다: 키 $key") }
        
        systemSettingRepository.deleteById(setting.id!!)
    }

    /**
     * 시스템 설정 조회
     */
    // @Cacheable(value = ["systemSettings"], key = "#key")
    fun getSystemSetting(key: String): SystemSettingResponse {
        val setting = systemSettingRepository.findByKey(key)
            .orElseThrow { ResourceNotFoundException("설정을 찾을 수 없습니다: 키 $key") }

        return SystemSettingResponse(
            id = setting.id ?: 0L,
            key = setting.key,
            value = setting.value,
            description = setting.description,
            createdAt = setting.createdAt,
            updatedAt = setting.updatedAt
        )
    }

    /**
     * 모든 시스템 설정 조회
     */
    // @Cacheable(value = ["systemSettings"], key = "'all'")
    fun getAllSystemSettings(): SystemSettingListResponse {
        val settings = systemSettingRepository.findAll()
        
        val responseList = settings.map { setting ->
            SystemSettingResponse(
                id = setting.id ?: 0L,
                key = setting.key,
                value = setting.value,
                description = setting.description,
                createdAt = setting.createdAt,
                updatedAt = setting.updatedAt
            )
        }

        return SystemSettingListResponse(
            settings = responseList,
            total = settings.size.toLong()
        )
    }

    /**
     * 이메일 설정 저장
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveEmailSettings(request: EmailSettingRequest) {
        val emailSettings = mapOf(
            "email.smtp.server" to request.smtpServer,
            "email.smtp.port" to request.smtpPort.toString(),
            "email.smtp.username" to request.username,
            "email.smtp.password" to request.password,
            "email.smtp.enableTLS" to request.enableTLS.toString(),
            "email.sender.name" to (request.senderName ?: "Church Attendly"),
            "email.template.prefix" to (request.emailTemplatePrefix ?: "emails/")
        )

        emailSettings.forEach { (key, value) ->
            saveSystemSetting(SystemSettingRequest(key = key, value = value))
        }
    }

    /**
     * Slack 설정 저장
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveSlackSettings(request: SlackSettingRequest) {
        val slackSettings = mapOf(
            "slack.webhook.url" to request.webhookUrl,
            "slack.channel.name" to (request.channelName ?: "church-attendly"),
            "slack.bot.name" to (request.botName ?: "Attendly Bot"),
            "slack.bot.icon.emoji" to (request.botIconEmoji ?: ":church:")
        )

        slackSettings.forEach { (key, value) ->
            if (value.isNotBlank()) {
                saveSystemSetting(SystemSettingRequest(key = key, value = value))
            }
        }
    }

    /**
     * 보안 정책 설정 저장
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveSecurityPolicies(request: SecurityPolicyRequest) {
        val securitySettings = mapOf(
            "security.session.timeout.minutes" to request.sessionTimeoutMinutes.toString(),
            "security.password.expiry.days" to request.passwordExpiryDays.toString(),
            "security.password.min.length" to request.minPasswordLength.toString(),
            "security.password.require.special" to request.requireSpecialChar.toString(),
            "security.password.require.uppercase" to request.requireUpperCase.toString(),
            "security.password.require.number" to request.requireNumber.toString(),
            "security.login.attempt.limit" to request.loginAttemptLimit.toString()
        )

        securitySettings.forEach { (key, value) ->
            saveSystemSetting(SystemSettingRequest(key = key, value = value))
        }
    }

    /**
     * 출석 입력 설정 저장
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveAttendanceSettings(request: AttendanceSettingRequest) {
        val attendanceSettings = mapOf(
            "attendance.input.day" to request.attendanceInputDayOfWeek.toString(),
            "attendance.input.start.hour" to request.inputStartHour.toString(),
            "attendance.input.end.hour" to request.inputEndHour.toString(),
            "attendance.allow.member.edit" to request.allowMemberEdit.toString(),
            "attendance.autolock.enabled" to request.autoLockInputEnabled.toString(),
            "attendance.autolock.timeout.hours" to request.autolockTimeoutHours.toString()
        )

        attendanceSettings.forEach { (key, value) ->
            saveSystemSetting(SystemSettingRequest(key = key, value = value))
        }
    }

    /**
     * 배치 작업 설정 저장
     */
    @Transactional
    @CacheEvict(value = ["systemSettings"], allEntries = true)
    fun saveBatchSettings(request: BatchSettingRequest) {
        val batchSettings = mapOf(
            "batch.reminder.enabled" to request.reminderEnabled.toString(),
            "batch.reminder.day" to request.reminderDayOfWeek.toString(),
            "batch.reminder.hour" to request.reminderHour.toString(),
            "batch.statistics.enabled" to request.statisticsGenerationEnabled.toString(),
            "batch.statistics.day" to request.statisticsGenerationDayOfWeek.toString(),
            "batch.statistics.hour" to request.statisticsGenerationHour.toString()
        )

        batchSettings.forEach { (key, value) ->
            saveSystemSetting(SystemSettingRequest(key = key, value = value))
        }
    }

    /**
     * 이메일 설정 조회
     */
    // @Cacheable(value = ["systemSettings"], key = "'emailSettings'")
    fun getEmailSettings(): EmailSettingRequest {
        val keys = listOf(
            "email.smtp.server",
            "email.smtp.port",
            "email.smtp.username",
            "email.smtp.password",
            "email.smtp.enableTLS",
            "email.sender.name",
            "email.template.prefix"
        )

        val settings = getSettingsMap(keys)

        return EmailSettingRequest(
            smtpServer = settings["email.smtp.server"] ?: "",
            smtpPort = settings["email.smtp.port"]?.toIntOrNull() ?: 587,
            username = settings["email.smtp.username"] ?: "",
            password = settings["email.smtp.password"] ?: "",
            enableTLS = settings["email.smtp.enableTLS"]?.toBoolean() ?: true,
            senderName = settings["email.sender.name"],
            emailTemplatePrefix = settings["email.template.prefix"]
        )
    }

    /**
     * 키 목록으로 설정 값 맵 반환
     */
    private fun getSettingsMap(keys: List<String>): Map<String, String> {
        val settings = systemSettingRepository.findByKeyIn(keys)
        return settings.associate { it.key to it.value }
    }
} 