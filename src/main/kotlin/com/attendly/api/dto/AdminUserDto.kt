package com.attendly.api.dto

import com.attendly.domain.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

data class UserCreateRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    @field:Email(message = "유효한 이메일 형식이 아닙니다")
    @field:NotBlank(message = "이메일은 필수입니다")
    val email: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    val password: String,

    @field:NotNull(message = "역할은 필수입니다")
    val role: Role,

    val birthDate: LocalDate? = null,

    @field:NotNull(message = "부서 ID는 필수입니다")
    val departmentId: Long
)

data class UserUpdateRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    @field:Email(message = "유효한 이메일 형식이 아닙니다")
    val email: String? = null,

    val birthDate: LocalDate? = null,

    val role: Role? = null,

    val departmentId: Long? = null
)

data class UserPasswordResetRequest(
    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상, 영문, 숫자, 특수문자를 포함해야 합니다"
    )
    val newPassword: String
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String?,
    val role: Role,
    val departmentId: Long,
    val departmentName: String,
    val birthDate: LocalDate?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class UserListResponse(
    val users: List<UserResponse>,
    val total: Long,
    val page: Int,
    val size: Int
)

data class UserBulkCreateRequest(
    @field:NotNull(message = "사용자 목록은 필수입니다")
    val users: List<UserCreateRequest>
)

data class UserBulkCreateResponse(
    val createdCount: Int,
    val failedCount: Int,
    val failedEmails: List<String>
) 