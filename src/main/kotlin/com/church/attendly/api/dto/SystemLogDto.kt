package com.church.attendly.api.dto

import com.church.attendly.domain.entity.SystemLog
import java.time.LocalDateTime

/**
 * 시스템 로그 응답 DTO
 */
data class SystemLogResponseDto(
    val id: Long?,
    val level: String,
    val category: String,
    val message: String,
    val additionalInfo: String?,
    val timestamp: LocalDateTime,
    val ipAddress: String?,
    val userId: Long?,
    val userAgent: String?,
    val serverInstance: String?
) {
    companion object {
        fun from(systemLog: SystemLog): SystemLogResponseDto {
            return SystemLogResponseDto(
                id = systemLog.id,
                level = systemLog.level,
                category = systemLog.category,
                message = systemLog.message,
                additionalInfo = systemLog.additionalInfo,
                timestamp = systemLog.timestamp,
                ipAddress = systemLog.ipAddress,
                userId = systemLog.userId,
                userAgent = systemLog.userAgent,
                serverInstance = systemLog.serverInstance
            )
        }
    }
}

/**
 * 시스템 로그 검색 DTO
 */
data class SystemLogSearchDto(
    val level: String? = null,
    val category: String? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val userId: Long? = null,
    val keyword: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String = "timestamp,desc"
)

/**
 * 시스템 로그 생성 DTO
 */
data class SystemLogCreateDto(
    val level: String,
    val category: String,
    val message: String,
    val additionalInfo: Any? = null
) 