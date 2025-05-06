package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.BatchJob
import com.attendly.domain.entity.BatchJobLog
import com.attendly.domain.repository.BatchJobRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
import com.attendly.exception.ErrorMessage
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

class AdminBatchServiceTest {

    private lateinit var batchJobRepository: BatchJobRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var adminBatchService: AdminBatchService

    @BeforeEach
    fun setUp() {
        batchJobRepository = mockk(relaxed = true)
        objectMapper = mockk(relaxed = true)
        adminBatchService = AdminBatchService(batchJobRepository, objectMapper)
    }

    @Test
    fun `createBatchJob should create and return a batch job with valid data`() {
        // given
        val request = BatchJobRequest(
            jobType = BatchJobType.GBS_REORGANIZATION,
            parameters = mapOf("key1" to "value1", "key2" to "value2"),
            scheduleTime = LocalDateTime.now().plusHours(1)
        )
        
        val parametersJson = """{"key1":"value1","key2":"value2"}"""
        
        val createdJob = BatchJob(
            id = 1L,
            jobType = BatchJobType.GBS_REORGANIZATION,
            status = BatchJobStatus.QUEUED,
            parameters = parametersJson,
            scheduleTime = request.scheduleTime!!,
            createdAt = LocalDateTime.now()
        )

        every { objectMapper.writeValueAsString(request.parameters) } returns parametersJson
        every { batchJobRepository.save(any()) } returns createdJob

        // when
        val result = adminBatchService.createBatchJob(request)

        // then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(BatchJobType.GBS_REORGANIZATION, result.jobType)
        assertEquals(BatchJobStatus.QUEUED, result.status)
        assertEquals(request.parameters, result.parameters)
        
        verify { objectMapper.writeValueAsString(request.parameters) }
        verify { batchJobRepository.save(any()) }
    }

    @Test
    fun `cancelBatchJob should throw exception when job not found`() {
        // given
        val jobId = 999L
        val request = BatchJobCancelRequest(reason = "테스트 취소")
        
        every { batchJobRepository.findById(jobId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminBatchService.cancelBatchJob(jobId, request)
        }
        
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.code, exception.errorCode)
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.message, exception.message)
        
        verify { batchJobRepository.findById(jobId) }
    }

    @Test
    fun `cancelBatchJob should throw exception when job already completed`() {
        // given
        val jobId = 1L
        val request = BatchJobCancelRequest(reason = "테스트 취소")
        
        val completedJob = BatchJob(
            id = jobId,
            jobType = BatchJobType.GBS_REORGANIZATION,
            status = BatchJobStatus.COMPLETED,
            parameters = "{}",
            scheduleTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now().minusHours(1),
            finishedAt = LocalDateTime.now()
        )
        
        every { batchJobRepository.findById(jobId) } returns Optional.of(completedJob)

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminBatchService.cancelBatchJob(jobId, request)
        }
        
        assertEquals(ErrorMessage.CANNOT_CANCEL_COMPLETED_JOB.code, exception.errorCode)
        assertEquals(ErrorMessage.CANNOT_CANCEL_COMPLETED_JOB.message, exception.message)
        
        verify { batchJobRepository.findById(jobId) }
    }

    @Test
    fun `restartBatchJob should throw exception when job running`() {
        // given
        val jobId = 1L
        val request = BatchJobRestartRequest(parameters = mapOf("key" to "value"))
        
        val runningJob = BatchJob(
            id = jobId,
            jobType = BatchJobType.GBS_REORGANIZATION,
            status = BatchJobStatus.RUNNING,
            parameters = "{}",
            scheduleTime = LocalDateTime.now(),
            createdAt = LocalDateTime.now().minusHours(1),
            startedAt = LocalDateTime.now().minusMinutes(30)
        )
        
        every { batchJobRepository.findById(jobId) } returns Optional.of(runningJob)

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminBatchService.restartBatchJob(jobId, request)
        }
        
        assertEquals(ErrorMessage.CANNOT_RESTART_RUNNING_JOB.code, exception.errorCode)
        assertEquals(ErrorMessage.CANNOT_RESTART_RUNNING_JOB.message, exception.message)
        
        verify { batchJobRepository.findById(jobId) }
    }

    @Test
    fun `getBatchJob should throw exception when job not found`() {
        // given
        val jobId = 999L
        
        every { batchJobRepository.findById(jobId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminBatchService.getBatchJob(jobId)
        }
        
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.code, exception.errorCode)
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.message, exception.message)
        
        verify { batchJobRepository.findById(jobId) }
    }

    @Test
    fun `getBatchJobLogs should throw exception when job not found`() {
        // given
        val jobId = 999L
        
        every { batchJobRepository.findById(jobId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminBatchService.getBatchJobLogs(jobId)
        }
        
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.code, exception.errorCode)
        assertEquals(ErrorMessage.BATCH_JOB_NOT_FOUND.message, exception.message)
        
        verify { batchJobRepository.findById(jobId) }
    }
} 