package com.attendly.api.controller

import com.attendly.api.dto.VillageLeaderAssignRequest
import com.attendly.api.dto.VillageLeaderResponse
import com.attendly.service.AdminVillageLeaderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/village-leader")
@Tag(name = "마을장 관리 API", description = "마을장 지정/해제 등의 관리 기능을 제공하는 API")
class AdminVillageLeaderController(
    private val adminVillageLeaderService: AdminVillageLeaderService
) {

    @PostMapping
    @Operation(
        summary = "마을장 등록",
        description = "특정 사용자를 특정 마을의 마을장으로 등록합니다. 이미 해당 마을에 마을장이 있는 경우 현재 마을장은 자동으로 해제됩니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun assignVillageLeader(
        @Valid @RequestBody request: VillageLeaderAssignRequest
    ): ResponseEntity<VillageLeaderResponse> {
        val response = adminVillageLeaderService.assignVillageLeader(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping("/{villageId}")
    @Operation(
        summary = "마을장 조회",
        description = "특정 마을의 현재 마을장 정보를 조회합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun getVillageLeader(
        @PathVariable villageId: Long
    ): ResponseEntity<VillageLeaderResponse> {
        val response = adminVillageLeaderService.getVillageLeader(villageId)
        return if (response != null) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{villageId}")
    @Operation(
        summary = "마을장 해제",
        description = "특정 마을의 현재 마을장을 해제합니다."
    )
    @PreAuthorize("hasRole('ADMIN')")
    fun terminateVillageLeader(
        @PathVariable villageId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<VillageLeaderResponse> {
        val response = adminVillageLeaderService.terminateVillageLeader(
            villageId = villageId,
            endDate = endDate ?: LocalDate.now()
        )
        return ResponseEntity.ok(response)
    }
} 