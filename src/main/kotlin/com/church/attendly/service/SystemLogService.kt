package com.church.attendly.service

import com.church.attendly.domain.entity.SystemLog
import com.church.attendly.domain.repository.SystemLogRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.net.InetAddress
import java.time.LocalDateTime
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class SystemLogService(
    private val systemLogRepository: SystemLogRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 시스템 로그를 생성하고 저장
     */
    fun createLog(
        level: String,
        category: String,
        message: String,
        additionalInfo: Any? = null,
        request: HttpServletRequest? = null
    ): SystemLog {
        val userId = getCurrentUserId()
        val ipAddress = request?.remoteAddr ?: "0.0.0.0"
        val userAgent = request?.getHeader("User-Agent")
        val serverInstance = InetAddress.getLocalHost().hostName

        val additionalInfoJson = additionalInfo?.let {
            objectMapper.writeValueAsString(it)
        }

        val systemLog = SystemLog(
            level = level,
            category = category,
            message = message,
            additionalInfo = additionalInfoJson,
            ipAddress = ipAddress,
            userId = userId,
            userAgent = userAgent,
            serverInstance = serverInstance
        )

        return systemLogRepository.save(systemLog)
    }

    /**
     * 현재 인증된 사용자의 ID를 가져옴
     */
    private fun getCurrentUserId(): Long? {
        val authentication: Authentication? = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            val principal = authentication.principal
            if (principal is org.springframework.security.core.userdetails.UserDetails) {
                return principal.username.toLongOrNull()
            }
        }
        return null
    }

    /**
     * 레벨별 로그 조회
     */
    fun getLogsByLevel(level: String, pageable: Pageable): Page<SystemLog> {
        return systemLogRepository.findByLevel(level, pageable)
    }

    /**
     * 카테고리별 로그 조회
     */
    fun getLogsByCategory(category: String, pageable: Pageable): Page<SystemLog> {
        return systemLogRepository.findByCategory(category, pageable)
    }

    /**
     * 기간별 로그 조회
     */
    fun getLogsByTimeRange(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<SystemLog> {
        return systemLogRepository.findByTimestampBetween(startTime, endTime, pageable)
    }

    /**
     * 복합 조건으로 로그 조회
     */
    fun getLogs(
        level: String? = null,
        category: String? = null,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        userId: Long? = null,
        keyword: String? = null,
        pageable: Pageable
    ): Page<SystemLog> {
        // QueryDSL 구현체를 통해 검색 수행
        return systemLogRepository.searchLogs(level, category, startTime, endTime, userId, keyword, pageable)
    }
    
    /**
     * ID로 시스템 로그 조회
     */
    fun getLogById(id: Long): SystemLog? {
        return systemLogRepository.findById(id).orElse(null)
    }
    
    /**
     * 시스템에 등록된 모든 로그 카테고리 조회
     */
    fun getLogCategories(): List<String> {
        return listOf(
            "APPLICATION",
            "SECURITY",
            "BATCH",
            "AUDIT",
            "ATTENDANCE",
            "USER_MANAGEMENT",
            "ORGANIZATION",
            "API_CALL"
        )
    }
} 