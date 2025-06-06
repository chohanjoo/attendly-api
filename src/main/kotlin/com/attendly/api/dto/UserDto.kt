package com.attendly.api.dto

import jakarta.validation.constraints.NotEmpty

/**
 * 역할 목록으로 사용자 조회 요청 DTO
 */
data class UserListByRolesRequest(
    @field:NotEmpty(message = "최소 하나 이상의 역할이 필요합니다")
    val roles: List<String>
)

/**
 * 역할 목록으로 사용자 조회 응답 DTO
 */
data class UserListByRolesResponse(
    val users: List<UserResponse>
) 