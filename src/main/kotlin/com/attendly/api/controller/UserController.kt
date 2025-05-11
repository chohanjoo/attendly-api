package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.UserListByRolesRequest
import com.attendly.api.dto.UserListByRolesResponse
import com.attendly.api.dto.UserVillageResponse
import com.attendly.api.util.ResponseUtil
import com.attendly.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자", description = "사용자 관련 API")
@SecurityRequirement(name = "bearerAuth")
class UserController(
    private val userService: UserService
) {

    @Operation(
        summary = "역할별 사용자 목록 조회",
        description = "지정된 역할을 가진 사용자 목록을 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/by-roles")
    @PreAuthorize("isAuthenticated()")
    fun getUsersByRoles(@Valid @RequestBody request: UserListByRolesRequest): ResponseEntity<ApiResponse<UserListByRolesResponse>> {
        val users = userService.getUsersByRoles(request)
        return ResponseUtil.success(UserListByRolesResponse(users))
    }
    
    @Operation(
        summary = "현재 사용자의 마을 정보 조회",
        description = "현재 로그인한 사용자가 속한 마을 정보를 조회합니다",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @GetMapping("/my-village")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUserVillage(authentication: Authentication): ResponseEntity<ApiResponse<UserVillageResponse>> {
        val villageResponse = userService.getCurrentUserVillage(authentication)
        return ResponseUtil.success(villageResponse)
    }
} 