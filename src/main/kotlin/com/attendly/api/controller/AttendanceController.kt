package com.attendly.api.controller

import com.attendly.api.dto.AttendanceBatchRequest
import com.attendly.api.dto.AttendanceResponse
import com.attendly.api.dto.VillageAttendanceResponse
import com.attendly.service.AttendanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "출석 API", description = "출석 입력 및 조회 API")
class AttendanceController(
    private val attendanceService: AttendanceService
) {

    @PostMapping("/attendance")
    @Operation(
        summary = "출석 일괄 등록",
        description = "리더 권한으로 출석 데이터를 일괄 등록합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("@methodSecurityExpressions.canManageGbsAttendance(#request.gbsId)")
    fun createAttendances(
        @Valid @RequestBody request: AttendanceBatchRequest
    ): ResponseEntity<List<AttendanceResponse>> {
        val attendances = attendanceService.createAttendances(request)
        return ResponseEntity.ok(attendances)
    }

    @GetMapping("/attendance")
    @Operation(
        summary = "GBS 출석 조회",
        description = "특정 GBS의 특정 주차 출석 데이터를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("isAuthenticated()")
    fun getAttendancesByGbs(
        @RequestParam gbsId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate
    ): ResponseEntity<List<AttendanceResponse>> {
        val attendances = attendanceService.getAttendancesByGbs(gbsId, weekStart)
        return ResponseEntity.ok(attendances)
    }

    @GetMapping("/village/{id}/attendance")
    @Operation(
        summary = "마을 출석 현황 조회",
        description = "특정 마을의 특정 주차 출석 현황을 집계하여 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("@methodSecurityExpressions.canAccessVillage(#id)")
    fun getVillageAttendance(
        @PathVariable id: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) weekStart: LocalDate
    ): ResponseEntity<VillageAttendanceResponse> {
        val villageAttendance = attendanceService.getVillageAttendance(id, weekStart)
        return ResponseEntity.ok(villageAttendance)
    }
} 