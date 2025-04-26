package com.church.attendly.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import java.io.IOException
import java.util.Collections

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var userDetailsService: UserDetailsService

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    @Mock
    private lateinit var securityContext: SecurityContext

    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilterForTest

    private val testToken = "test-jwt-token"
    private val testUsername = "testuser"

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        jwtAuthenticationFilter = JwtAuthenticationFilterForTest(jwtTokenProvider, userDetailsService)
    }

    @Test
    fun `인증 헤더가 없을 때 필터가 계속 진행되어야 한다`() {
        // Given
        `when`(request.getHeader("Authorization")).thenReturn(null)

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(filterChain, times(1)).doFilter(request, response)
        verify(jwtTokenProvider, never()).extractUsername(anyString())
    }

    @Test
    fun `Bearer 형식이 아닌 인증 헤더가 있을 때 필터가 계속 진행되어야 한다`() {
        // Given
        `when`(request.getHeader("Authorization")).thenReturn("InvalidFormat")

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(filterChain, times(1)).doFilter(request, response)
        verify(jwtTokenProvider, never()).extractUsername(anyString())
    }

    @Test
    fun `토큰 추출 중 예외가 발생하면 필터가 계속 진행되어야 한다`() {
        // Given
        `when`(request.getHeader("Authorization")).thenReturn("Bearer $testToken")
        `when`(jwtTokenProvider.extractUsername(testToken)).thenThrow(RuntimeException("Token extraction failed"))

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(filterChain, times(1)).doFilter(request, response)
        verify(userDetailsService, never()).loadUserByUsername(anyString())
    }

    @Test
    fun `유효한 토큰으로 사용자 인증이 성공해야 한다`() {
        // Given
        val userDetails = User(testUsername, "", Collections.emptyList())
        
        // SecurityContext 설정
        SecurityContextHolder.setContext(securityContext)
        
        `when`(request.getHeader("Authorization")).thenReturn("Bearer $testToken")
        `when`(jwtTokenProvider.extractUsername(testToken)).thenReturn(testUsername)
        `when`(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails)
        `when`(jwtTokenProvider.validateToken(testToken, userDetails)).thenReturn(true)

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(filterChain, times(1)).doFilter(request, response)
        verify(userDetailsService, times(1)).loadUserByUsername(testUsername)
        verify(jwtTokenProvider, times(1)).validateToken(testToken, userDetails)
        verify(securityContext).authentication = any(UsernamePasswordAuthenticationToken::class.java)
    }

    @Test
    fun `토큰이 유효하지 않으면 인증이 설정되지 않아야 한다`() {
        // Given
        val userDetails = User(testUsername, "", Collections.emptyList())
        
        // SecurityContext 설정
        SecurityContextHolder.setContext(securityContext)
        
        `when`(request.getHeader("Authorization")).thenReturn("Bearer $testToken")
        `when`(jwtTokenProvider.extractUsername(testToken)).thenReturn(testUsername)
        `when`(userDetailsService.loadUserByUsername(testUsername)).thenReturn(userDetails)
        `when`(jwtTokenProvider.validateToken(testToken, userDetails)).thenReturn(false)

        // When
        jwtAuthenticationFilter.doFilterInternalForTest(request, response, filterChain)

        // Then
        verify(filterChain, times(1)).doFilter(request, response)
        verify(userDetailsService, times(1)).loadUserByUsername(testUsername)
        verify(jwtTokenProvider, times(1)).validateToken(testToken, userDetails)
        verify(securityContext, never()).authentication = any()
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