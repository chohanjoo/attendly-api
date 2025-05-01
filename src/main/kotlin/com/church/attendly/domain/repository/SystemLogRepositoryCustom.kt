package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.SystemLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * SystemLogRepository 커스텀 인터페이스
 */
interface SystemLogRepositoryCustom {
    
    /**
     * 복합 조건에 맞는 시스템 로그 검색
     */
    fun searchLogs(
        level: String?,
        category: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        userId: Long?,
        keyword: String?,
        pageable: Pageable
    ): Page<SystemLog>
} 