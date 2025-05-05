package com.church.attendly.api.dto

import com.church.attendly.domain.entity.SystemLog
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
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
    val serverInstance: String?,
    // API 로그 정보
    val apiInfo: ApiCallInfoDto? = null
) {
    companion object {
        fun from(systemLog: SystemLog): SystemLogResponseDto {
            // API_CALL 카테고리인 경우 추가 정보를 파싱
            val apiInfo = if (systemLog.category == "API_CALL" && systemLog.additionalInfo != null) {
                try {
                    val objectMapper = ObjectMapper()
                    val additionalInfoMap = objectMapper.readValue(systemLog.additionalInfo, Map::class.java)
                    
                    ApiCallInfoDto(
                        requestId = additionalInfoMap["requestId"] as? String,
                        apiPath = additionalInfoMap["apiPath"] as? String,
                        method = additionalInfoMap["method"] as? String,
                        status = (additionalInfoMap["status"] as? Int)?.let { HttpStatus.valueOf(it) },
                        duration = (additionalInfoMap["duration"] as? Number)?.toLong(),
                        requestBody = additionalInfoMap["requestBody"] as? String,
                        responseBody = additionalInfoMap["responseBody"] as? String
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
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
                serverInstance = systemLog.serverInstance,
                apiInfo = apiInfo
            )
        }
    }
}

/**
 * API 호출 정보 DTO
 */
data class ApiCallInfoDto(
    val requestId: String? = null,
    val apiPath: String? = null,
    val method: String? = null,
    val status: HttpStatus? = null,
    val duration: Long? = null,
    val requestBody: String? = null,
    val responseBody: String? = null
)

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