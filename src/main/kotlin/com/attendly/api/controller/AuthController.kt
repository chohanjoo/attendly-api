package com.attendly.api.controller

import com.attendly.api.dto.LoginRequest
import com.attendly.api.dto.LoginResponse
import com.attendly.api.dto.SignupRequest
import com.attendly.api.dto.SignupResponse
import com.attendly.api.dto.TokenRefreshRequest
import com.attendly.api.dto.TokenRefreshResponse
import com.attendly.api.dto.UserResponse
import com.attendly.security.JwtTokenProvider
import com.attendly.security.UserDetailsAdapter
import com.attendly.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/auth")
@Tag(name = "인증 API", description = "회원가입, 로그인 및 토큰 갱신 API")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService,
    private val userService: UserService
) {

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<SignupResponse> {
        val response = userService.signup(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    fun login(@Valid @RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        )
        
        val userDetails = authentication.principal as UserDetailsAdapter
        val user = userDetails.getUser()
        
        val accessToken = jwtTokenProvider.generateToken(userDetails)
        val refreshToken = jwtTokenProvider.generateRefreshToken(userDetails)
        
        val response = LoginResponse(
            userId = user.id ?: 0L,
            name = user.name,
            role = user.role.name,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
        
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 엑세스 토큰과 리프레시 토큰을 발급받습니다.")
    fun refreshToken(@Valid @RequestBody request: TokenRefreshRequest): ResponseEntity<TokenRefreshResponse> {
        val username = jwtTokenProvider.extractUsername(request.refreshToken)
        val userDetails = userDetailsService.loadUserByUsername(username)
        
        if (jwtTokenProvider.validateToken(request.refreshToken, userDetails)) {
            val accessToken = jwtTokenProvider.generateToken(userDetails)
            val refreshToken = jwtTokenProvider.generateRefreshToken(userDetails)
            
            return ResponseEntity.ok(TokenRefreshResponse(accessToken, refreshToken))
        }
        
        return ResponseEntity.badRequest().build()
    }
    
    @GetMapping("/me")
    @Operation(summary = "현재 사용자 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetailsAdapter): ResponseEntity<UserResponse> {
        val user = userDetails.getUser()
        
        val response = UserResponse(
            id = user.id ?: 0L,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            role = user.role,
            departmentId = user.department.id ?: 0L,
            departmentName = user.department.name,
            birthDate = user.birthDate,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
        
        return ResponseEntity.ok(response)
    }
} 