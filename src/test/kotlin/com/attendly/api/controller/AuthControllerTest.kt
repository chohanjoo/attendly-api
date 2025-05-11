package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.LoginRequest
import com.attendly.api.dto.LoginResponse
import com.attendly.api.dto.SignupRequest
import com.attendly.api.dto.SignupResponse
import com.attendly.api.dto.UserResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.security.JwtTokenProvider
import com.attendly.security.UserDetailsAdapter
import com.attendly.service.UserService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class AuthControllerTest {

    private lateinit var controller: AuthController
    private lateinit var userService: UserService
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userDetailsService: UserDetailsService
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setup() {
        userService = mockk()
        authenticationManager = mockk()
        jwtTokenProvider = mockk()
        userDetailsService = mockk()
        authentication = mockk()
        
        controller = AuthController(
            authenticationManager,
            jwtTokenProvider,
            userDetailsService,
            userService
        )
    }

    @Test
    fun `회원가입 성공 테스트`() {
        // Given
        val signupRequest = SignupRequest(
            email = "test@example.com",
            password = "password123",
            name = "홍길동",
            phoneNumber = "010-1234-5678",
            role = Role.LEADER,
            departmentId = 1L
        )

        val signupResponse = SignupResponse(
            userId = 1L,
            name = "홍길동",
            email = "test@example.com",
            phoneNumber = "010-1234-5678",
            role = "LEADER"
        )

        every { userService.signup(signupRequest) } returns signupResponse

        // When
        val result = controller.signup(signupRequest)

        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(signupResponse, result.body?.data)
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
        every { user.phoneNumber } returns "010-1234-5678"
        every { user.role } returns Role.LEADER

        val userDetailsAdapter = mockk<UserDetailsAdapter>()
        every { userDetailsAdapter.getUser() } returns user
        every { userDetailsAdapter.getPassword() } returns "password"
        every { userDetailsAdapter.getUsername() } returns "test@example.com"
        every { userDetailsAdapter.isAccountNonExpired() } returns true
        every { userDetailsAdapter.isAccountNonLocked() } returns true
        every { userDetailsAdapter.isCredentialsNonExpired() } returns true
        every { userDetailsAdapter.isEnabled() } returns true
        every { userDetailsAdapter.getAuthorities() } returns listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MEMBER"))

        val authentication = mockk<Authentication>()
        every { authentication.principal } returns userDetailsAdapter

        val authToken = UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
        every { authenticationManager.authenticate(authToken) } returns authentication
        every { jwtTokenProvider.generateToken(userDetailsAdapter) } returns "access-token"
        every { jwtTokenProvider.generateRefreshToken(userDetailsAdapter) } returns "refresh-token"

        // When
        val result = controller.login(loginRequest)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(1L, result.body?.data?.userId)
        assertEquals("홍길동", result.body?.data?.name)
        assertEquals("LEADER", result.body?.data?.role)
        assertEquals("access-token", result.body?.data?.accessToken)
        assertEquals("refresh-token", result.body?.data?.refreshToken)
    }

    @Test
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
        every { user.phoneNumber } returns "010-1234-5678"
        every { user.role } returns Role.MEMBER
        every { user.department } returns department
        every { user.birthDate } returns LocalDate.of(1990, 1, 1)
        every { user.createdAt } returns now
        every { user.updatedAt } returns now
        every { user.password } returns "password123"
        
        val userDetailsAdapter = mockk<UserDetailsAdapter>()
        every { userDetailsAdapter.getUser() } returns user

        // When
        val result = controller.getCurrentUser(userDetailsAdapter)
        
        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(1L, result.body?.data?.id)
        assertEquals("테스트 사용자", result.body?.data?.name)
        assertEquals("test@example.com", result.body?.data?.email)
        assertEquals("010-1234-5678", result.body?.data?.phoneNumber)
        assertEquals(Role.MEMBER, result.body?.data?.role)
        assertEquals(1L, result.body?.data?.departmentId)
        assertEquals("테스트 부서", result.body?.data?.departmentName)
    }
} 