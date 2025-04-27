package com.church.attendly.api.dto

import com.church.attendly.domain.entity.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// 로그인 요청 DTO
data class LoginRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이 아닙니다")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String
)

// 로그인 응답 DTO
data class LoginResponse(
    val userId: Long,
    val name: String,
    val role: String,
    val accessToken: String,
    val refreshToken: String
)

// 회원가입 요청 DTO
data class SignupRequest(
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "유효한 이메일 형식이 아닙니다")
    val email: String,
    
    @field:NotBlank(message = "비밀번호는 필수입니다")
    val password: String,
    
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,
    
    @field:NotNull(message = "역할은 필수입니다")
    val role: Role,
    
    @field:NotNull(message = "부서 ID는 필수입니다")
    val departmentId: Long
)

// 회원가입 응답 DTO
data class SignupResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val role: String
)

// 토큰 갱신 요청 DTO
data class TokenRefreshRequest(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다")
    val refreshToken: String
)

// 토큰 갱신 응답 DTO
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String
) 