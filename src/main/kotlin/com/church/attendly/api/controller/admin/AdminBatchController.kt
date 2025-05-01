package com.church.attendly.api.controller.admin

import com.church.attendly.api.dto.*
import com.church.attendly.service.AdminBatchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/batch")
@Tag(name = "관리자-배치작업", description = "관리자 전용 배치 작업 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminBatchController(
    private val adminBatchService: AdminBatchService
) {

    @Operation(
        summary = "배치 작업 생성", 
        description = "새로운 배치 작업을 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/jobs")
    fun createBatchJob(@Valid @RequestBody request: BatchJobRequest): ResponseEntity<BatchJobResponse> {
        val response = adminBatchService.createBatchJob(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "배치 작업 취소", 
        description = "실행 중이거나 대기 중인 배치 작업을 취소합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/jobs/{jobId}/cancel")
    fun cancelBatchJob(
        @PathVariable jobId: Long,
        @RequestBody request: BatchJobCancelRequest?
    ): ResponseEntity<BatchJobResponse> {
        val response = adminBatchService.cancelBatchJob(jobId, request)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "배치 작업 재시작", 
        description = "완료되거나 실패한 배치 작업을 재시작합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/jobs/{jobId}/restart")
    fun restartBatchJob(
        @PathVariable jobId: Long,
        @RequestBody request: BatchJobRestartRequest?
    ): ResponseEntity<BatchJobResponse> {
        val response = adminBatchService.restartBatchJob(jobId, request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "배치 작업 조회", 
        description = "특정 배치 작업을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/jobs/{jobId}")
    fun getBatchJob(@PathVariable jobId: Long): ResponseEntity<BatchJobResponse> {
        val response = adminBatchService.getBatchJob(jobId)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "배치 작업 목록 조회", 
        description = "배치 작업 목록을 페이징하여 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/jobs")
    fun getBatchJobs(
        @RequestParam(required = false) jobType: BatchJobType?,
        @RequestParam(required = false) status: BatchJobStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDateFrom: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDateTo: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDateFrom: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDateTo: LocalDateTime?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<BatchJobResponse>> {
        val response = adminBatchService.getBatchJobs(
            jobType = jobType,
            status = status,
            startDateFrom = startDateFrom,
            startDateTo = startDateTo,
            endDateFrom = endDateFrom,
            endDateTo = endDateTo,
            pageable = pageable
        )
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "배치 작업 로그 조회", 
        description = "특정 배치 작업의 실행 로그를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/jobs/{jobId}/logs")
    fun getBatchJobLogs(@PathVariable jobId: Long): ResponseEntity<List<BatchLogResponse>> {
        val response = adminBatchService.getBatchJobLogs(jobId)
        return ResponseEntity(response, HttpStatus.OK)
    }
} 