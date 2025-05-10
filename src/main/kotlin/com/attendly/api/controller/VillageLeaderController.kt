package com.attendly.api.controller

import com.attendly.api.dto.AttendanceResponse
import com.attendly.api.dto.AttendanceUpdateRequestDto
import com.attendly.api.dto.VillageGbsInfoResponse
import com.attendly.service.AttendanceService
import com.attendly.service.OrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/village-leader")
@Tag(name = "마을장 API", description = "마을장을 위한 API")
class VillageLeaderController(
    private val organizationService: OrganizationService,
    private val attendanceService: AttendanceService
) {
    @GetMapping("/{villageId}/gbs")
    @Operation(
        summary = "마을내 모든 GBS 정보 조회",
        description = "마을장이 자신의 마을에 속한 모든 GBS 그룹과 각 GBS의 리더, 조원들의 정보를 조회합니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MINISTER', 'VILLAGE_LEADER') and @securityUtils.isVillageLeaderOf(#villageId)")
    fun getVillageGbsInfo(
        @PathVariable villageId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<VillageGbsInfoResponse> {
        val response = organizationService.getVillageGbsInfo(
            villageId = villageId,
            date = date ?: LocalDate.now()
        )
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/{villageId}/attendance")
    @Operation(
        summary = "마을 내 GBS 출석 데이터 수정",
        description = "마을장이 자신의 마을에 속한 GBS의 출석 데이터를 수정합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MINISTER', 'VILLAGE_LEADER') and @securityUtils.isVillageLeaderOf(#villageId)")
    fun updateVillageGbsAttendance(
        @PathVariable villageId: Long,
        @Valid @RequestBody request: AttendanceUpdateRequestDto
    ): ResponseEntity<List<AttendanceResponse>> {
        val response = attendanceService.updateVillageGbsAttendance(villageId, request)
        return ResponseEntity.ok(response)
    }
} 