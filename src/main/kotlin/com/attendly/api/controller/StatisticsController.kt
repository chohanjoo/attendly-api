package com.attendly.api.controller

import com.attendly.api.dto.DepartmentStatistics
import com.attendly.api.dto.GbsStatistics
import com.attendly.api.dto.VillageStatistics
import com.attendly.service.StatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "통계 API", description = "출석 통계 조회 API")
class StatisticsController(
    private val statisticsService: StatisticsService
) {

    @GetMapping("/departments/{id}/report")
    @Operation(
        summary = "부서 출석 통계 조회",
        description = "특정 부서의 출석 통계를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("@methodSecurityExpressions.isMinister(authentication) or hasRole('ADMIN')")
    fun getDepartmentStatistics(
        @PathVariable id: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<DepartmentStatistics> {
        val statistics = statisticsService.getDepartmentStatistics(id, startDate, endDate)
        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/villages/{id}/report")
    @Operation(
        summary = "마을 출석 통계 조회",
        description = "특정 마을의 출석 통계를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("@methodSecurityExpressions.canAccessVillage(#id)")
    fun getVillageStatistics(
        @PathVariable id: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<VillageStatistics> {
        val statistics = statisticsService.getVillageStatistics(id, startDate, endDate)
        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/gbs/{id}/report")
    @Operation(
        summary = "GBS 출석 통계 조회",
        description = "특정 GBS의 출석 통계를 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("@methodSecurityExpressions.canManageGbsAttendance(#id) or hasAnyRole('ADMIN', 'MINISTER')")
    fun getGbsStatistics(
        @PathVariable id: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<GbsStatistics> {
        val statistics = statisticsService.getGbsStatistics(id, startDate, endDate)
        return ResponseEntity.ok(statistics)
    }
} 