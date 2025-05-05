package com.church.attendly.api.controller.admin

import com.church.attendly.api.dto.SystemLogResponseDto
import com.church.attendly.api.dto.SystemLogSearchDto
import com.church.attendly.domain.entity.SystemLog
import com.church.attendly.service.SystemLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "관리자 로그 API", description = "시스템 로그 조회 관련 API")
@PreAuthorize("hasAuthority('ADMIN')")
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
    ): ResponseEntity<Page<SystemLogResponseDto>> {
        val sortParams = sort.split(",")
        val direction = if (sortParams.size > 1 && sortParams[1] == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sortProperty = sortParams[0]
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty))

        val logs = systemLogService.getLogs(level, category, startTime, endTime, userId, keyword, pageable)
        val responseDto = logs.map { SystemLogResponseDto.from(it) }
        
        return ResponseEntity.ok(responseDto)
    }
    
    @GetMapping("/api-calls")
    @Operation(
        summary = "API 호출 로그 조회", 
        description = "API 호출 관련 로그만 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getApiCallLogs(
        @RequestParam(required = false) level: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime?,
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) apiPath: String?,
        @RequestParam(required = false) method: String?,
        @RequestParam(required = false) status: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "timestamp,desc") sort: String
    ): ResponseEntity<Page<SystemLogResponseDto>> {
        val sortParams = sort.split(",")
        val direction = if (sortParams.size > 1 && sortParams[1] == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val sortProperty = sortParams[0]
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty))

        // apiPath, method, status는 키워드 검색에 포함시킴
        val keywordParts = mutableListOf<String>()
        apiPath?.let { keywordParts.add("apiPath:$it") }
        method?.let { keywordParts.add("method:$it") }
        status?.let { keywordParts.add("status:$it") }
        
        val keyword = if (keywordParts.isNotEmpty()) keywordParts.joinToString(" ") else null
        
        val logs = systemLogService.getLogs(level, "API_CALL", startTime, endTime, userId, keyword, pageable)
        val responseDto = logs.map { SystemLogResponseDto.from(it) }
        
        return ResponseEntity.ok(responseDto)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "시스템 로그 단건 조회", 
        description = "특정 ID의 시스템 로그를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogById(@PathVariable id: Long): ResponseEntity<SystemLogResponseDto> {
        val log = systemLogService.getLogById(id)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(SystemLogResponseDto.from(log))
    }

    @GetMapping("/categories")
    @Operation(
        summary = "로그 카테고리 목록 조회", 
        description = "시스템에 등록된 모든 로그 카테고리를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogCategories(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(systemLogService.getLogCategories())
    }

    @GetMapping("/levels")
    @Operation(
        summary = "로그 레벨 목록 조회", 
        description = "시스템에 등록된 모든 로그 레벨을 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getLogLevels(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(listOf("INFO", "WARN", "ERROR", "DEBUG", "TRACE"))
    }
} 