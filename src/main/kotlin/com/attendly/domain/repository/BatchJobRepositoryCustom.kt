package com.attendly.domain.repository

import com.attendly.api.dto.BatchJobStatus
import com.attendly.api.dto.BatchJobType
import com.attendly.domain.entity.BatchJob
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface BatchJobRepositoryCustom {
    fun findJobsByFilters(
        jobType: BatchJobType? = null,
        status: BatchJobStatus? = null,
        startDateFrom: LocalDateTime? = null,
        startDateTo: LocalDateTime? = null,
        endDateFrom: LocalDateTime? = null,
        endDateTo: LocalDateTime? = null,
        pageable: Pageable
    ): Page<BatchJob>
} 