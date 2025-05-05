package com.church.attendly.api.controller

import com.church.attendly.api.dto.UserListByRolesRequest
import com.church.attendly.api.dto.UserListByRolesResponse
import com.church.attendly.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    fun getUsersByRoles(@Valid @RequestBody request: UserListByRolesRequest): ResponseEntity<UserListByRolesResponse> {
        val users = userService.getUsersByRoles(request)
        return ResponseEntity.ok(UserListByRolesResponse(users))
    }
} 