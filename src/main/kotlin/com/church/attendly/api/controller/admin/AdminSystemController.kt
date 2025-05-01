package com.church.attendly.api.controller.admin

import com.church.attendly.api.dto.*
import com.church.attendly.service.AdminSystemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/system")
@Tag(name = "관리자-시스템설정", description = "관리자 전용 시스템 설정 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminSystemController(
    private val adminSystemService: AdminSystemService
) {

    @Operation(
        summary = "시스템 설정 저장", 
        description = "시스템 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/settings")
    fun saveSystemSetting(@Valid @RequestBody request: SystemSettingRequest): ResponseEntity<SystemSettingResponse> {
        val response = adminSystemService.saveSystemSetting(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "시스템 설정 삭제", 
        description = "시스템 설정을 삭제합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/settings/{key}")
    fun deleteSystemSetting(@PathVariable key: String): ResponseEntity<Void> {
        adminSystemService.deleteSystemSetting(key)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @Operation(
        summary = "시스템 설정 조회", 
        description = "특정 시스템 설정을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/settings/{key}")
    fun getSystemSetting(@PathVariable key: String): ResponseEntity<SystemSettingResponse> {
        val response = adminSystemService.getSystemSetting(key)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "모든 시스템 설정 조회", 
        description = "모든 시스템 설정을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/settings")
    fun getAllSystemSettings(): ResponseEntity<SystemSettingListResponse> {
        val response = adminSystemService.getAllSystemSettings()
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "이메일 설정 저장", 
        description = "이메일 서비스 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/email-settings")
    fun saveEmailSettings(@Valid @RequestBody request: EmailSettingRequest): ResponseEntity<Void> {
        adminSystemService.saveEmailSettings(request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "이메일 설정 조회", 
        description = "이메일 서비스 설정을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/email-settings")
    fun getEmailSettings(): ResponseEntity<EmailSettingRequest> {
        val response = adminSystemService.getEmailSettings()
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "Slack 설정 저장", 
        description = "Slack 알림 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/slack-settings")
    fun saveSlackSettings(@Valid @RequestBody request: SlackSettingRequest): ResponseEntity<Void> {
        adminSystemService.saveSlackSettings(request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "보안 정책 설정 저장", 
        description = "보안 정책 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/security-policies")
    fun saveSecurityPolicies(@Valid @RequestBody request: SecurityPolicyRequest): ResponseEntity<Void> {
        adminSystemService.saveSecurityPolicies(request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "출석 입력 설정 저장", 
        description = "출석 입력 관련 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/attendance-settings")
    fun saveAttendanceSettings(@Valid @RequestBody request: AttendanceSettingRequest): ResponseEntity<Void> {
        adminSystemService.saveAttendanceSettings(request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "배치 작업 설정 저장", 
        description = "배치 작업 관련 설정을 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/batch-settings")
    fun saveBatchSettings(@Valid @RequestBody request: BatchSettingRequest): ResponseEntity<Void> {
        adminSystemService.saveBatchSettings(request)
        return ResponseEntity(HttpStatus.OK)
    }
} 