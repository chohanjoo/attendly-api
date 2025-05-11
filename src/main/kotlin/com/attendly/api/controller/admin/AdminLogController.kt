package com.attendly.api.controller.admin

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.SystemLogResponseDto
import com.attendly.api.util.ResponseUtil
import com.attendly.service.SystemLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "관리자 로그 API", description = "시스템 로그 조회 관련 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminLogController(
    private val systemLogService: SystemLogService
) {

    @GetMapping
    @Operation(
        summary = "시스템 로그 조회", 
        description = "조건별 시스템 로그를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogs(
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "timestamp,desc") sort: String
    ): ResponseEntity<ApiResponse<PageResponse<SystemLogResponseDto>>> {
        val sortParams = sort.split(",")
        val direction = if (sortParams.size > 1 && sortParams[1] == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sortProperty = sortParams[0]
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty))

        val logs = systemLogService.getLogs(level, category, startTime, endTime, userId, keyword, pageable)
        val responseDto = logs.map { SystemLogResponseDto.from(it) }
        
        return ResponseUtil.successList(
            responseDto.content,
            responseDto.totalElements,
            responseDto.hasNext()
        )
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "시스템 로그 단건 조회", 
        description = "특정 ID의 시스템 로그를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogById(@PathVariable id: Long): ResponseEntity<ApiResponse<SystemLogResponseDto>> {
        val log = systemLogService.getLogById(id)
            ?: return ResponseUtil.error("로그를 찾을 수 없습니다.", 404, HttpStatus.NOT_FOUND)
        
        return ResponseUtil.success(SystemLogResponseDto.from(log))
    }

    @GetMapping("/categories")
    @Operation(
        summary = "로그 카테고리 목록 조회", 
        description = "시스템에 등록된 모든 로그 카테고리를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogCategories(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseUtil.success(systemLogService.getLogCategories())
    }

    @GetMapping("/levels")
    @Operation(
        summary = "로그 레벨 목록 조회", 
        description = "시스템에 등록된 모든 로그 레벨을 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogLevels(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseUtil.success(listOf("INFO", "WARN", "ERROR", "DEBUG", "TRACE"))
    }
} 