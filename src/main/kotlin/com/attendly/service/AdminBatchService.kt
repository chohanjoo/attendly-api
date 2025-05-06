package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.BatchJob
import com.attendly.domain.entity.BatchJobLog
import com.attendly.domain.repository.BatchJobRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AdminBatchService(
    private val batchJobRepository: BatchJobRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 배치 작업 생성
     */
    @Transactional
    fun createBatchJob(request: BatchJobRequest): BatchJobResponse {
        val parametersJson = objectMapper.writeValueAsString(request.parameters)

        val batchJob = BatchJob(
            jobType = request.jobType,
            status = BatchJobStatus.QUEUED,
            parameters = parametersJson,
            scheduleTime = request.scheduleTime ?: LocalDateTime.now()
        )

        val savedJob = batchJobRepository.save(batchJob)
        savedJob.addLog("작업이 생성되었습니다", "INFO")

        return BatchJobResponse(
            id = savedJob.id ?: 0L,
            jobType = savedJob.jobType,
            status = savedJob.status,
            parameters = request.parameters,
            createdAt = savedJob.createdAt,
            startedAt = savedJob.startedAt,
            finishedAt = savedJob.finishedAt,
            errorMessage = savedJob.errorMessage
        )
    }

    /**
     * 배치 작업 취소
     */
    @Transactional
    fun cancelBatchJob(jobId: Long, request: BatchJobCancelRequest?): BatchJobResponse {
        val batchJob = batchJobRepository.findById(jobId)
            .orElseThrow { AttendlyApiException(ErrorMessage.BATCH_JOB_NOT_FOUND.code, ErrorMessage.BATCH_JOB_NOT_FOUND.message) }

        if (batchJob.status == BatchJobStatus.COMPLETED || batchJob.status == BatchJobStatus.FAILED) {
            throw AttendlyApiException(ErrorMessage.CANNOT_CANCEL_COMPLETED_JOB.code, ErrorMessage.CANNOT_CANCEL_COMPLETED_JOB.message)
        }

        val updatedJob = BatchJob(
            id = batchJob.id,
            jobType = batchJob.jobType,
            status = BatchJobStatus.CANCELLED,
            parameters = batchJob.parameters,
            createdAt = batchJob.createdAt,
            startedAt = batchJob.startedAt,
            finishedAt = LocalDateTime.now(),
            errorMessage = "사용자에 의해 취소됨: ${request?.reason ?: "이유 없음"}",
            scheduleTime = batchJob.scheduleTime
        )

        val savedJob = batchJobRepository.save(updatedJob)
        savedJob.addLog("작업이 취소되었습니다: ${request?.reason ?: "이유 없음"}", "WARN")

        val parameters = try {
            objectMapper.readValue(savedJob.parameters, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        return BatchJobResponse(
            id = savedJob.id ?: 0L,
            jobType = savedJob.jobType,
            status = savedJob.status,
            parameters = parameters,
            createdAt = savedJob.createdAt,
            startedAt = savedJob.startedAt,
            finishedAt = savedJob.finishedAt,
            errorMessage = savedJob.errorMessage
        )
    }

    /**
     * 배치 작업 재시작
     */
    @Transactional
    fun restartBatchJob(jobId: Long, request: BatchJobRestartRequest?): BatchJobResponse {
        val batchJob = batchJobRepository.findById(jobId)
            .orElseThrow { AttendlyApiException(ErrorMessage.BATCH_JOB_NOT_FOUND.code, ErrorMessage.BATCH_JOB_NOT_FOUND.message) }

        if (batchJob.status == BatchJobStatus.RUNNING) {
            throw AttendlyApiException(ErrorMessage.CANNOT_RESTART_RUNNING_JOB.code, ErrorMessage.CANNOT_RESTART_RUNNING_JOB.message)
        }

        val parametersMap = try {
            objectMapper.readValue(batchJob.parameters, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        // 새로운 파라미터가 있으면 병합
        val mergedParameters = if (request?.parameters != null) {
            parametersMap + request.parameters
        } else {
            parametersMap
        }

        val parametersJson = objectMapper.writeValueAsString(mergedParameters)

        val newJob = BatchJob(
            jobType = batchJob.jobType,
            status = BatchJobStatus.QUEUED,
            parameters = parametersJson,
            scheduleTime = LocalDateTime.now()
        )

        val savedJob = batchJobRepository.save(newJob)
        savedJob.addLog("작업 ID ${batchJob.id}에서 재시작되었습니다", "INFO")

        return BatchJobResponse(
            id = savedJob.id ?: 0L,
            jobType = savedJob.jobType,
            status = savedJob.status,
            parameters = mergedParameters,
            createdAt = savedJob.createdAt,
            startedAt = savedJob.startedAt,
            finishedAt = savedJob.finishedAt,
            errorMessage = savedJob.errorMessage
        )
    }

    /**
     * 배치 작업 조회
     */
    fun getBatchJob(jobId: Long): BatchJobResponse {
        val batchJob = batchJobRepository.findById(jobId)
            .orElseThrow { AttendlyApiException(ErrorMessage.BATCH_JOB_NOT_FOUND.code, ErrorMessage.BATCH_JOB_NOT_FOUND.message) }

        val parameters = try {
            objectMapper.readValue(batchJob.parameters, Map::class.java) as Map<String, String>
        } catch (e: Exception) {
            emptyMap<String, String>()
        }

        return BatchJobResponse(
            id = batchJob.id ?: 0L,
            jobType = batchJob.jobType,
            status = batchJob.status,
            parameters = parameters,
            createdAt = batchJob.createdAt,
            startedAt = batchJob.startedAt,
            finishedAt = batchJob.finishedAt,
            errorMessage = batchJob.errorMessage
        )
    }

    /**
     * 배치 작업 목록 조회
     */
    fun getBatchJobs(
        jobType: BatchJobType? = null,
        status: BatchJobStatus? = null,
        startDateFrom: LocalDateTime? = null,
        startDateTo: LocalDateTime? = null,
        endDateFrom: LocalDateTime? = null,
        endDateTo: LocalDateTime? = null,
        pageable: Pageable
    ): Page<BatchJobResponse> {
        return batchJobRepository.findJobsByFilters(
            jobType = jobType,
            status = status,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            endDateFrom = endDateFrom,
            endDateTo = endDateTo,
            pageable = pageable
        ).map { batchJob ->
            val parameters = try {
                objectMapper.readValue(batchJob.parameters, Map::class.java) as Map<String, String>
            } catch (e: Exception) {
                emptyMap<String, String>()
            }

            BatchJobResponse(
                id = batchJob.id ?: 0L,
                jobType = batchJob.jobType,
                status = batchJob.status,
                parameters = parameters,
                createdAt = batchJob.createdAt,
                startedAt = batchJob.startedAt,
                finishedAt = batchJob.finishedAt,
                errorMessage = batchJob.errorMessage
            )
        }
    }

    /**
     * 배치 작업 로그 조회
     */
    fun getBatchJobLogs(jobId: Long): List<BatchLogResponse> {
        val batchJob = batchJobRepository.findById(jobId)
            .orElseThrow { AttendlyApiException(ErrorMessage.BATCH_JOB_NOT_FOUND.code, ErrorMessage.BATCH_JOB_NOT_FOUND.message) }

        return batchJob.logs.map { log ->
            BatchLogResponse(
                id = log.id ?: 0L,
                jobId = jobId,
                message = log.message,
                level = log.level,
                timestamp = log.timestamp
            )
        }.sortedByDescending { it.timestamp }
    }
} 