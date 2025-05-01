package com.church.attendly.api.controller.admin

import com.church.attendly.api.dto.*
import com.church.attendly.service.AdminUserService
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
    fun createUser(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        val response = adminUserService.createUser(request)
        return ResponseEntity(response, HttpStatus.CREATED)
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
    ): ResponseEntity<UserResponse> {
        val response = adminUserService.updateUser(userId, request)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "사용자 삭제", 
        description = "사용자를 삭제합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: Long): ResponseEntity<Void> {
        adminUserService.deleteUser(userId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @Operation(
        summary = "사용자 조회", 
        description = "특정 사용자 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/{userId}")
    fun getUser(@PathVariable userId: Long): ResponseEntity<UserResponse> {
        val response = adminUserService.getUser(userId)
        return ResponseEntity(response, HttpStatus.OK)
    }

    @Operation(
        summary = "사용자 목록 조회", 
        description = "사용자 목록을 페이징하여 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping
    fun getAllUsers(
        @PageableDefault(size = 20, sort = ["id"], direction = Sort.Direction.DESC) pageable: Pageable
    ): ResponseEntity<Page<UserResponse>> {
        val response = adminUserService.getAllUsers(pageable)
        return ResponseEntity(response, HttpStatus.OK)
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
    ): ResponseEntity<Void> {
        adminUserService.resetPassword(userId, request)
        return ResponseEntity(HttpStatus.OK)
    }

    @Operation(
        summary = "사용자 일괄 등록", 
        description = "여러 사용자를 일괄적으로 등록합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/bulk")
    fun bulkCreateUsers(@Valid @RequestBody request: UserBulkCreateRequest): ResponseEntity<UserBulkCreateResponse> {
        val response = adminUserService.bulkCreateUsers(request)
        return ResponseEntity(response, HttpStatus.CREATED)
    }
} 