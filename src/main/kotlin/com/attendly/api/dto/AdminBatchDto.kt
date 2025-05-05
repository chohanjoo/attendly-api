package com.attendly.api.dto

import java.time.LocalDateTime

enum class BatchJobStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
}

enum class BatchJobType {
    GBS_REORGANIZATION, STATISTICS_GENERATION, REMINDER, DATA_BACKUP
}

data class BatchJobRequest(
    val jobType: BatchJobType,
    val parameters: Map<String, String> = emptyMap(),
    val scheduleTime: LocalDateTime? = null
)

data class BatchJobResponse(
    val id: Long,
    val jobType: BatchJobType,
    val status: BatchJobStatus,
    val parameters: Map<String, String>,
    val createdAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val errorMessage: String?
)

data class BatchJobListResponse(
    val jobs: List<BatchJobResponse>,
    val total: Long,
    val page: Int,
    val size: Int
)

data class BatchLogResponse(
    val id: Long,
    val jobId: Long,
    val message: String,
    val level: String,
    val timestamp: LocalDateTime
)

data class BatchJobCancelRequest(
    val reason: String? = null
)

data class BatchJobRestartRequest(
    val parameters: Map<String, String>? = null
) 