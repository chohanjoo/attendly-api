package com.attendly.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.DayOfWeek
import java.time.LocalDateTime

data class SystemSettingRequest(
    @field:NotBlank(message = "설정 키는 필수입니다")
    @field:Size(min = 2, max = 50, message = "설정 키는 2자 이상 50자 이하로 입력해주세요")
    val key: String,

    @field:NotBlank(message = "설정 값은 필수입니다")
    @field:Size(max = 1000, message = "설정 값은 1000자 이하로 입력해주세요")
    val value: String,

    @field:Size(max = 200, message = "설명은 200자 이하로 입력해주세요")
    val description: String? = null
)

data class SystemSettingResponse(
    val id: Long,
    val key: String,
    val value: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class SystemSettingListResponse(
    val settings: List<SystemSettingResponse>,
    val total: Long
)

data class EmailSettingRequest(
    @field:NotBlank(message = "SMTP 서버는 필수입니다")
    val smtpServer: String,

    @field:NotNull(message = "SMTP 포트는 필수입니다")
    val smtpPort: Int,

    @field:NotBlank(message = "이메일 계정은 필수입니다")
    @field:Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "유효한 이메일 형식이 아닙니다")
    val username: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,

    val enableTLS: Boolean = true,
    val senderName: String? = null,
    val emailTemplatePrefix: String? = null
)

data class SlackSettingRequest(
    @field:NotBlank(message = "웹훅 URL은 필수입니다")
    val webhookUrl: String,

    val channelName: String? = null,
    val botName: String? = null,
    val botIconEmoji: String? = null
)

data class SecurityPolicyRequest(
    @field:NotNull(message = "세션 타임아웃(분)은 필수입니다")
    val sessionTimeoutMinutes: Int,

    @field:NotNull(message = "비밀번호 만료일(일)은 필수입니다")
    val passwordExpiryDays: Int,

    @field:NotNull(message = "비밀번호 최소 길이는 필수입니다")
    val minPasswordLength: Int,

    val requireSpecialChar: Boolean = true,
    val requireUpperCase: Boolean = true,
    val requireNumber: Boolean = true,
    val loginAttemptLimit: Int = 5
)

data class AttendanceSettingRequest(
    @field:NotNull(message = "입력 가능 요일은 필수입니다")
    val attendanceInputDayOfWeek: DayOfWeek,

    @field:NotNull(message = "입력 시작 시간은 필수입니다")
    val inputStartHour: Int,

    @field:NotNull(message = "입력 종료 시간은 필수입니다")
    val inputEndHour: Int,

    val allowMemberEdit: Boolean = false,
    val autoLockInputEnabled: Boolean = true,
    val autolockTimeoutHours: Int = 48
)

data class BatchSettingRequest(
    val reminderEnabled: Boolean = true,
    val reminderDayOfWeek: DayOfWeek = DayOfWeek.SATURDAY,
    val reminderHour: Int = 12,
    val statisticsGenerationEnabled: Boolean = true,
    val statisticsGenerationDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val statisticsGenerationHour: Int = 2
) 