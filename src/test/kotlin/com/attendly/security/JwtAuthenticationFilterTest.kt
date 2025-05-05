package com.attendly.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import java.io.IOException
import java.util.Collections
import io.mockk.junit5.MockKExtension
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK

@ExtendWith(MockKExtension::class)
class JwtAuthenticationFilterTest {

    @MockK
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockK
    private lateinit var userDetailsService: UserDetailsService

    @RelaxedMockK
    private lateinit var request: HttpServletRequest

    @RelaxedMockK
    private lateinit var response: HttpServletResponse

    @RelaxedMockK
    private lateinit var filterChain: FilterChain

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilterForTest

    private val testToken = "test-jwt-token"
    private val testUsername = "testuser"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        
        SecurityContextHolder.clearContext()
        
        // JwtAuthenticationFilter 생성
        jwtAuthenticationFilter = JwtAuthenticationFilterForTest(jwtTokenProvider, userDetailsService)
    }

    @Test
    fun `인증 헤더가 없을 때 필터가 계속 진행되어야 한다`() {
        // Given
        every { request.getHeader("Authorization") } returns null

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 0) { jwtTokenProvider.extractUsername(any()) }
    }

    @Test
    fun `Bearer 형식이 아닌 인증 헤더가 있을 때 필터가 계속 진행되어야 한다`() {
        // Given
        every { request.getHeader("Authorization") } returns "InvalidFormat"

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 0) { jwtTokenProvider.extractUsername(any()) }
    }

    @Test
    fun `토큰 추출 중 예외가 발생하면 필터가 계속 진행되어야 한다`() {
        // Given
        every { request.getHeader("Authorization") } returns "Bearer $testToken"
        every { jwtTokenProvider.extractUsername(testToken) } throws RuntimeException("Token extraction failed")

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 0) { userDetailsService.loadUserByUsername(any()) }
    }

    @Test
    fun `유효한 토큰으로 사용자 인증이 성공해야 한다`() {
        // Given
        val userDetails = User(testUsername, "", Collections.emptyList())
        
        mockkStatic(SecurityContextHolder::class)
        val mockContext = mockk<SecurityContext>(relaxed = true)
        every { SecurityContextHolder.getContext() } returns mockContext
        every { mockContext.authentication } returns null
        
        every { request.getHeader("Authorization") } returns "Bearer $testToken"
        every { jwtTokenProvider.extractUsername(testToken) } returns testUsername
        every { userDetailsService.loadUserByUsername(testUsername) } returns userDetails
        every { jwtTokenProvider.validateToken(testToken, userDetails) } returns true
        
        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 1) { userDetailsService.loadUserByUsername(testUsername) }
        verify(exactly = 1) { jwtTokenProvider.validateToken(testToken, userDetails) }
        verify(exactly = 1) { mockContext.setAuthentication(any()) }
    }

    @Test
    fun `토큰이 유효하지 않으면 인증이 설정되지 않아야 한다`() {
        // Given
        val userDetails = User(testUsername, "", Collections.emptyList())
        
        mockkStatic(SecurityContextHolder::class)
        val mockContext = mockk<SecurityContext>(relaxed = true)
        every { SecurityContextHolder.getContext() } returns mockContext
        every { mockContext.authentication } returns null
        
        every { request.getHeader("Authorization") } returns "Bearer $testToken"
        every { jwtTokenProvider.extractUsername(testToken) } returns testUsername
        every { userDetailsService.loadUserByUsername(testUsername) } returns userDetails
        every { jwtTokenProvider.validateToken(testToken, userDetails) } returns false

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(exactly = 1) { filterChain.doFilter(request, response) }
        verify(exactly = 1) { userDetailsService.loadUserByUsername(testUsername) }
        verify(exactly = 1) { jwtTokenProvider.validateToken(testToken, userDetails) }
        verify(exactly = 0) { mockContext.setAuthentication(any()) }
    }

    // 테스트를 위한 하위 클래스
    class JwtAuthenticationFilterForTest(
        jwtTokenProvider: JwtTokenProvider,
        userDetailsService: UserDetailsService
    ) : JwtAuthenticationFilter(jwtTokenProvider, userDetailsService) {
        
        @Throws(ServletException::class, IOException::class)
        fun doFilterInternalForTest(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            super.doFilterInternal(request, response, filterChain)
        }
    }
} 