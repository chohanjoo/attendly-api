package com.attendly.domain.repository

import com.attendly.api.dto.BatchJobStatus
import com.attendly.api.dto.BatchJobType
import com.attendly.domain.entity.BatchJob
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface BatchJobRepository : JpaRepository<BatchJob, Long>, BatchJobRepositoryCustom {
    fun findByStatus(status: BatchJobStatus, pageable: Pageable): Page<BatchJob>
    fun findByJobType(jobType: BatchJobType, pageable: Pageable): Page<BatchJob>
    fun findByJobTypeAndStatus(jobType: BatchJobType, status: BatchJobStatus, pageable: Pageable): Page<BatchJob>
    
    @Query("SELECT b FROM BatchJob b WHERE b.scheduleTime <= :now AND b.status = :status")
    fun findReadyToExecuteJobs(now: LocalDateTime, status: BatchJobStatus = BatchJobStatus.QUEUED): List<BatchJob>
} 