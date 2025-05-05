package com.church.attendly.api.dto

import com.church.attendly.domain.entity.Role
import jakarta.validation.constraints.NotEmpty
import java.util.*

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