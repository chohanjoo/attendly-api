package com.attendly.api.controller.admin

import com.attendly.api.dto.*
import com.attendly.api.util.ResponseUtil
import com.attendly.service.AdminOrganizationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
    fun createDepartment(@Valid @RequestBody request: DepartmentCreateRequest): ResponseEntity<ApiResponse<DepartmentResponse>> {
        val response = adminOrganizationService.createDepartment(request)
        return ResponseUtil.created(response, "부서가 성공적으로 생성되었습니다")
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
    ): ResponseEntity<ApiResponse<DepartmentResponse>> {
        val response = adminOrganizationService.updateDepartment(departmentId, request)
        return ResponseUtil.success(response, "부서 정보가 수정되었습니다")
    }

    @Operation(
        summary = "부서 삭제", 
        description = "부서를 삭제합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/departments/{departmentId}")
    fun deleteDepartment(@PathVariable departmentId: Long): ResponseEntity<ApiResponse<Void>> {
        adminOrganizationService.deleteDepartment(departmentId)
        return ResponseUtil.successNoData(message = "부서가 성공적으로 삭제되었습니다", status = HttpStatus.NO_CONTENT)
    }

    @Operation(
        summary = "부서 조회", 
        description = "특정 부서 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/departments/{departmentId}")
    fun getDepartment(@PathVariable departmentId: Long): ResponseEntity<ApiResponse<DepartmentResponse>> {
        val response = adminOrganizationService.getDepartment(departmentId)
        return ResponseUtil.success(response)
    }

    @Operation(
        summary = "부서 목록 조회", 
        description = "모든 부서를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/departments")
    fun getAllDepartments(): ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> {
        val response = adminOrganizationService.getAllDepartments()
        return ResponseUtil.successList(response)
    }

    // 마을 관리 API
    @Operation(
        summary = "마을 생성", 
        description = "새로운 마을을 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/villages")
    fun createVillage(@Valid @RequestBody request: VillageCreateRequest): ResponseEntity<ApiResponse<VillageResponse>> {
        val response = adminOrganizationService.createVillage(request)
        return ResponseUtil.created(response, "마을이 성공적으로 생성되었습니다")
    }

    @Operation(
        summary = "마을 목록 조회", 
        description = "모든 마을 목록을 조회합니다. 부서별, 이름으로 필터링이 가능합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/villages")
    fun getAllVillages(
        @RequestParam(required = false) departmentId: Long?,
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PageResponse<VillageResponse>>> {
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val response = adminOrganizationService.getAllVillages(departmentId, name, pageable)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(response, "마을 목록 조회 성공"))
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
    ): ResponseEntity<ApiResponse<VillageResponse>> {
        val response = adminOrganizationService.updateVillage(villageId, request)
        return ResponseUtil.success(response, "마을 정보가 수정되었습니다")
    }

    // GBS 그룹 관리 API
    @Operation(
        summary = "GBS 그룹 생성", 
        description = "새로운 GBS 그룹을 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/gbs-groups")
    fun createGbsGroup(@Valid @RequestBody request: GbsGroupCreateRequest): ResponseEntity<ApiResponse<GbsGroupResponse>> {
        val response = adminOrganizationService.createGbsGroup(request)
        return ResponseUtil.created(response, "GBS 그룹이 성공적으로 생성되었습니다")
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
    ): ResponseEntity<ApiResponse<GbsGroupResponse>> {
        val response = adminOrganizationService.updateGbsGroup(gbsGroupId, request)
        return ResponseUtil.success(response, "GBS 그룹 정보가 수정되었습니다")
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
    ): ResponseEntity<ApiResponse<Void>> {
        adminOrganizationService.assignLeaderToGbs(gbsGroupId, request)
        return ResponseUtil.successNoData(message = "GBS 그룹에 리더가 성공적으로 배정되었습니다")
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
    ): ResponseEntity<ApiResponse<Void>> {
        adminOrganizationService.assignMemberToGbs(gbsGroupId, request)
        return ResponseUtil.successNoData(message = "GBS 그룹에 조원이 성공적으로 배정되었습니다")
    }

    @Operation(
        summary = "GBS 6개월 주기 재편성 실행", 
        description = "GBS 그룹의 6개월 주기 재편성을 실행합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/reorganization")
    fun executeGbsReorganization(
        @Valid @RequestBody request: GbsReorganizationRequest
    ): ResponseEntity<ApiResponse<GbsReorganizationResponse>> {
        val response = adminOrganizationService.executeGbsReorganization(request)
        return ResponseUtil.success(response, "GBS 그룹 재편성이 성공적으로 실행되었습니다")
    }
} 