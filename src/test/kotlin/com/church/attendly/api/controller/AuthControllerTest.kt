package com.church.attendly.api.controller

import com.church.attendly.api.dto.LoginRequest
import com.church.attendly.api.dto.SignupRequest
import com.church.attendly.api.dto.SignupResponse
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.security.JwtTokenProvider
import com.church.attendly.security.TestSecurityConfig
import com.church.attendly.security.UserDetailsAdapter
import com.church.attendly.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AuthController::class)
@Import(TestSecurityConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var userDetailsService: UserDetailsService

    @Test
    fun `회원가입 성공 테스트`() {
        // Given
        val signupRequest = SignupRequest(
            email = "test@example.com",
            password = "password123",
            name = "홍길동",
            role = Role.LEADER,
            departmentId = 1L
        )

        val signupResponse = SignupResponse(
            userId = 1L,
            name = "홍길동",
            email = "test@example.com",
            role = "LEADER"
        )

        every { userService.signup(signupRequest) } returns signupResponse

        // When & Then
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
            .andExpect(jsonPath("$.role").value("LEADER"))
    }

    @Test
    fun `회원가입 유효성 검증 실패 테스트`() {
        // Given
        val invalidSignupRequest = SignupRequest(
            email = "",
            password = "",
            name = "",
            role = Role.LEADER,
            departmentId = 1L
        )

        // When & Then
        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidSignupRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `로그인 성공 테스트`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        val user = mockk<User>()
        every { user.id } returns 1L
        every { user.name } returns "홍길동"
        every { user.role } returns Role.LEADER

        val userDetailsAdapter = mockk<UserDetailsAdapter>()
        every { userDetailsAdapter.getUser() } returns user

        val authentication = mockk<Authentication>()
        every { authentication.principal } returns userDetailsAdapter

        val authToken = UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        every { authenticationManager.authenticate(authToken) } returns authentication
        every { jwtTokenProvider.generateToken(userDetailsAdapter) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(userDetailsAdapter) } returns "refresh-token"

        // When & Then
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1L))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.role").value("LEADER"))
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
    }

    @Test
    @Disabled("현재 UserResponse 변경으로 인해 테스트 수정 필요")
    fun `현재 사용자 정보 조회 테스트`() {
        // Given
        val department = mockk<Department>()
        every { department.id } returns 1L
        every { department.name } returns "테스트 부서"
        
        val now = LocalDateTime.now()
        val user = mockk<User>()
        every { user.id } returns 1L
        every { user.name } returns "테스트 사용자"
        every { user.email } returns "test@example.com"
        every { user.role } returns Role.MEMBER
        every { user.department } returns department
        every { user.birthDate } returns LocalDate.of(1990, 1, 1)
        every { user.createdAt } returns now
        every { user.updatedAt } returns now
        
        val userDetailsAdapter = mockk<UserDetailsAdapter>()
        every { userDetailsAdapter.getUser() } returns user
        
        // When & Then
        mockMvc.perform(get("/auth/me")
            .with(user(userDetailsAdapter)))
            .andExpect(status().isOk)
    }
} 