package com.attendly.api.controller.admin

import com.attendly.api.dto.*
import com.attendly.api.util.ResponseUtil
import com.attendly.enums.Role
import com.attendly.service.AdminUserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "관리자-사용자", description = "관리자 전용 사용자 관리 API")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    @Operation(
        summary = "사용자 생성", 
        description = "새로운 사용자를 생성합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping
    fun createUser(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val response = adminUserService.createUser(request)
        return ResponseUtil.created(response)
    }

    @Operation(
        summary = "사용자 수정", 
        description = "사용자 정보를 수정합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val response = adminUserService.updateUser(userId, request)
        return ResponseUtil.success(response)
    }

    @Operation(
        summary = "사용자 삭제", 
        description = "사용자를 삭제합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<ApiResponse<Void>> {
        adminUserService.deleteUser(userId)
        return ResponseUtil.successNoData(status = HttpStatus.NO_CONTENT)
    }

    @Operation(
        summary = "사용자 조회", 
        description = "특정 사용자 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val response = adminUserService.getUser(userId)
        return ResponseUtil.success(response)
    }

    @Operation(
        summary = "사용자 목록 조회", 
        description = "사용자 목록을 페이징하여 조회합니다. name 파라미터를 통해 이름으로 검색이 가능합니다. departmentId로 부서별 필터링, villageId로 마을별 필터링, roles로 역할 필터링이 가능합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping
    fun getAllUsers(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) departmentId: Long?,
        @RequestParam(required = false) villageId: Long?,
        @RequestParam(required = false) roles: List<Role>?,
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<ApiResponse<PageResponse<UserResponse>>> {
        val response = adminUserService.searchUsers(name, departmentId, villageId, roles, pageable)
        return ResponseUtil.successList(response.content, response.totalElements, response.hasNext())
    }

    @Operation(
        summary = "비밀번호 초기화", 
        description = "사용자의 비밀번호를 초기화합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/{userId}/reset-password")
    fun resetPassword(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UserPasswordResetRequest
    ): ResponseEntity<ApiResponse<Void>> {
        adminUserService.resetPassword(userId, request)
        return ResponseUtil.successNoData()
    }

    @Operation(
        summary = "사용자 일괄 등록", 
        description = "여러 사용자를 일괄적으로 등록합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/bulk")
    fun bulkCreateUsers(@Valid @RequestBody request: UserBulkCreateRequest): ResponseEntity<ApiResponse<UserBulkCreateResponse>> {
        val response = adminUserService.bulkCreateUsers(request)
        return ResponseUtil.created(response)
    }

    @Operation(
        summary = "사용자 상태 변경", 
        description = "사용자의 상태(활성/비활성)를 변경합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PatchMapping("/{userId}/status")
    fun updateUserStatus(
        @PathVariable userId: Long,
        @Valid @RequestBody request: UserStatusUpdateRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val response = adminUserService.updateUserStatus(userId, request)
        return ResponseUtil.success(response)
    }
} 