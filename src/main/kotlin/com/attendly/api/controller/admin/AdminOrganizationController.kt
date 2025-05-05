package com.attendly.api.controller.admin

import com.attendly.api.dto.*
import com.attendly.service.AdminOrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/organization")
@Tag(name = "관리자-조직구조", description = "관리자 전용 조직 구조 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminOrganizationController(
    private val adminOrganizationService: AdminOrganizationService
) {

    // 부서 관리 API
    @Operation(
        summary = "부서 생성", 
        description = "새로운 부서를 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/departments")
    fun createDepartment(@Valid @RequestBody request: DepartmentCreateRequest): ResponseEntity<DepartmentResponse> {
        val response = adminOrganizationService.createDepartment(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "부서 수정", 
        description = "부서 정보를 수정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/departments/{departmentId}")
    fun updateDepartment(
        @PathVariable departmentId: Long,
        @Valid @RequestBody request: DepartmentUpdateRequest
    ): ResponseEntity<DepartmentResponse> {
        val response = adminOrganizationService.updateDepartment(departmentId, request)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "부서 삭제", 
        description = "부서를 삭제합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/departments/{departmentId}")
    fun deleteDepartment(@PathVariable departmentId: Long): ResponseEntity<Void> {
        adminOrganizationService.deleteDepartment(departmentId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @Operation(
        summary = "부서 조회", 
        description = "특정 부서 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/departments/{departmentId}")
    fun getDepartment(@PathVariable departmentId: Long): ResponseEntity<DepartmentResponse> {
        val response = adminOrganizationService.getDepartment(departmentId)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "부서 목록 조회", 
        description = "모든 부서를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/departments")
    fun getAllDepartments(): ResponseEntity<List<DepartmentResponse>> {
        val response = adminOrganizationService.getAllDepartments()
        return ResponseEntity(response, HttpStatus.OK)
    }

    // 마을 관리 API
    @Operation(
        summary = "마을 생성", 
        description = "새로운 마을을 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/villages")
    fun createVillage(@Valid @RequestBody request: VillageCreateRequest): ResponseEntity<VillageResponse> {
        val response = adminOrganizationService.createVillage(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "마을 수정", 
        description = "마을 정보를 수정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/villages/{villageId}")
    fun updateVillage(
        @PathVariable villageId: Long,
        @Valid @RequestBody request: VillageUpdateRequest
    ): ResponseEntity<VillageResponse> {
        val response = adminOrganizationService.updateVillage(villageId, request)
        return ResponseEntity(response, HttpStatus.OK)
    }

    // GBS 그룹 관리 API
    @Operation(
        summary = "GBS 그룹 생성", 
        description = "새로운 GBS 그룹을 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/gbs-groups")
    fun createGbsGroup(@Valid @RequestBody request: GbsGroupCreateRequest): ResponseEntity<GbsGroupResponse> {
        val response = adminOrganizationService.createGbsGroup(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }

    @Operation(
        summary = "GBS 그룹 수정", 
        description = "GBS 그룹 정보를 수정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/gbs-groups/{gbsGroupId}")
    fun updateGbsGroup(
        @PathVariable gbsGroupId: Long,
        @Valid @RequestBody request: GbsGroupUpdateRequest
    ): ResponseEntity<GbsGroupResponse> {
        val response = adminOrganizationService.updateGbsGroup(gbsGroupId, request)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "GBS 그룹에 리더 배정", 
        description = "GBS 그룹에 리더를 배정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/gbs-groups/{gbsGroupId}/leaders")
    fun assignLeaderToGbs(
        @PathVariable gbsGroupId: Long,
        @Valid @RequestBody request: GbsLeaderAssignRequest
    ): ResponseEntity<Void> {
        adminOrganizationService.assignLeaderToGbs(gbsGroupId, request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "GBS 그룹에 조원 배정", 
        description = "GBS 그룹에 조원을 배정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/gbs-groups/{gbsGroupId}/members")
    fun assignMemberToGbs(
        @PathVariable gbsGroupId: Long,
        @Valid @RequestBody request: GbsMemberAssignRequest
    ): ResponseEntity<Void> {
        adminOrganizationService.assignMemberToGbs(gbsGroupId, request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "GBS 6개월 주기 재편성 실행", 
        description = "GBS 그룹의 6개월 주기 재편성을 실행합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/reorganization")
    fun executeGbsReorganization(
        @Valid @RequestBody request: GbsReorganizationRequest
    ): ResponseEntity<GbsReorganizationResponse> {
        val response = adminOrganizationService.executeGbsReorganization(request)
        return ResponseEntity(response, HttpStatus.OK)
    }
} 