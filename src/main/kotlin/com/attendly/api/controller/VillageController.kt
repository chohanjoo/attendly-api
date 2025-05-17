package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.UserVillageResponse
import com.attendly.api.dto.GbsAssignmentResponse
import com.attendly.api.dto.GbsAssignmentSaveRequest
import com.attendly.api.dto.GbsAssignmentSaveResponse
import com.attendly.api.dto.GbsGroupListResponse
import com.attendly.api.dto.LeaderCandidateResponse
import com.attendly.api.dto.VillageMemberResponse
import com.attendly.api.util.ResponseUtil
import com.attendly.service.VillageService
import com.attendly.service.GbsMemberService
import com.attendly.service.GbsGroupService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/village")
@Tag(name = "마을", description = "마을 관련 API")
@SecurityRequirement(name = "bearerAuth")
class VillageController(
    private val villageService: VillageService,
    private val gbsMemberService: GbsMemberService,
    private val gbsGroupService: GbsGroupService
) {
    
    @Operation(
        summary = "특정 마을 정보 조회",
        description = "특정 마을의 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getVillageById(@PathVariable id: Long, authentication: Authentication): ResponseEntity<ApiResponse<UserVillageResponse>> {
        val villageResponse = villageService.getVillageById(id, authentication)
        return ResponseUtil.success(villageResponse, "마을 정보 조회 성공")
    }
    
    @Operation(
        summary = "마을 멤버 목록 조회",
        description = "마을에 속한 멤버 목록을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{villageId}/members")
    @PreAuthorize("isAuthenticated()")
    fun getVillageMembers(@PathVariable villageId: Long): ResponseEntity<ApiResponse<VillageMemberResponse>> {
        val membersResponse = villageService.getVillageMembers(villageId)
        return ResponseUtil.success(membersResponse, "마을 멤버 목록 조회 성공")
    }
    
    @Operation(
        summary = "GBS 배치 정보 조회",
        description = "GBS 배치 관리를 위한 칸반 보드에 필요한 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{id}/gbs-assignment")
    @PreAuthorize("isAuthenticated()")
    fun getGbsAssignment(
        @PathVariable id: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<ApiResponse<GbsAssignmentResponse>> {
        val targetDate = date ?: LocalDate.now()
        val assignmentResponse = gbsMemberService.getGbsAssignment(id, targetDate)
        return ResponseUtil.success(assignmentResponse, "GBS 배치 정보 조회 성공")
    }
    
    @Operation(
        summary = "GBS 배치 정보 저장",
        description = "GBS 배치 관리를 위한 칸반 보드에서 설정한 배치 정보를 저장합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/{id}/gbs-assignment")
    @PreAuthorize("isAuthenticated()")
    fun saveGbsAssignment(
        @PathVariable id: Long,
        @RequestBody request: GbsAssignmentSaveRequest
    ): ResponseEntity<ApiResponse<GbsAssignmentSaveResponse>> {
        val response = gbsMemberService.saveGbsAssignment(id, request)
        return ResponseUtil.success(response, "GBS 배치 저장 성공")
    }
    
    @Operation(
        summary = "GBS 리더 후보 목록 조회",
        description = "GBS 리더로 지정할 수 있는 사용자 목록을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{id}/leader-candidates")
    @PreAuthorize("isAuthenticated()")
    fun getLeaderCandidates(@PathVariable id: Long): ResponseEntity<ApiResponse<LeaderCandidateResponse>> {
        val candidatesResponse = gbsMemberService.getLeaderCandidates(id)
        return ResponseUtil.success(candidatesResponse, "리더 후보 목록 조회 성공")
    }
    
    @Operation(
        summary = "GBS 그룹 목록 조회",
        description = "특정 마을의 GBS 그룹 목록을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{id}/gbs-groups")
    @PreAuthorize("isAuthenticated()")
    fun getGbsGroups(@PathVariable id: Long): ResponseEntity<ApiResponse<GbsGroupListResponse>> {
        val groupsResponse = gbsGroupService.getGbsGroups(id)
        return ResponseUtil.success(groupsResponse, "GBS 그룹 목록 조회 성공")
    }
} 