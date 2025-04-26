package com.church.attendly.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

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