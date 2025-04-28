package com.church.attendly.security

import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userDetailsAdapter: UserDetailsAdapter

    private val secretKey = "thisIsASecretKeyForTestingPurposesOnlyAndMustBeLongEnoughForHS256Algorithm"
    private val expirationTime = 3600000L // 1시간
    private val refreshExpirationTime = 86400000L // 24시간
    private val userEmail = "test@example.com"
    private val userId = 1L

    @BeforeEach
    fun setUp() {
        // 실제 JwtTokenProvider 객체 생성
        jwtTokenProvider = JwtTokenProvider(secretKey, expirationTime, refreshExpirationTime)
        
        // User와 UserDetailsAdapter 객체 생성
        val mockUser = mockk<User>()
        every { mockUser.id } returns userId
        every { mockUser.email } returns userEmail
        
        // UserDetailsAdapter 생성 (spyk를 사용하여 실제 메서드 호출 가능)
        userDetailsAdapter = mockk<UserDetailsAdapter>()
        every { userDetailsAdapter.username } returns userEmail
        every { userDetailsAdapter.getId() } returns userId
        every { userDetailsAdapter.getUser() } returns mockUser
    }

    @Test
    fun `generateToken 메서드는 유효한 토큰을 생성한다`() {
        // when
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }
    
    @Test
    fun `extractUsername 메서드는 토큰에서 사용자 이름을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // when
        val username = jwtTokenProvider.extractUsername(token)
        
        // then
        assertEquals(userEmail, username)
    }
    
    @Test
    fun `extractExpiration 메서드는 토큰에서 만료 시간을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // when
        val expiration = jwtTokenProvider.extractExpiration(token)
        
        // then
        assertNotNull(expiration)
        assertTrue(expiration.time > System.currentTimeMillis())
    }
    
    @Test
    fun `validateToken 메서드는 유효한 토큰에 대해 true를 반환한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // when
        val isValid = jwtTokenProvider.validateToken(token, userDetailsAdapter)
        
        // then
        assertTrue(isValid)
    }
    
    @Test
    fun `validateToken 메서드는 만료된 토큰에 대해 예외를 발생시킨다`() {
        // given - 1초 후에 만료되는 토큰 생성
        val shortExpirationTime = 1000L // 1초
        val tempProvider = JwtTokenProvider(secretKey, shortExpirationTime, refreshExpirationTime)
        val token = tempProvider.generateToken(userDetailsAdapter)
        
        // 토큰이 만료되도록 1.5초 대기
        Thread.sleep(1500)
        
        // when & then
        assertThrows(ExpiredJwtException::class.java) {
            tempProvider.extractUsername(token)
        }
    }
    
    @Test
    fun `validateToken 메서드는 다른 사용자의 토큰에 대해 false를 반환한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // 다른 사용자 정보로 UserDetails 생성
        val otherUserDetails = mockk<UserDetailsAdapter>()
        every { otherUserDetails.username } returns "other@example.com"
        
        // when
        val isValid = jwtTokenProvider.validateToken(token, otherUserDetails)
        
        // then
        assertFalse(isValid)
    }
    
    @Test
    fun `generateRefreshToken 메서드는 유효한 리프레시 토큰을 생성한다`() {
        // when
        val refreshToken = jwtTokenProvider.generateRefreshToken(userDetailsAdapter)
        
        // then
        assertNotNull(refreshToken)
        assertTrue(refreshToken.isNotEmpty())
        
        val expiration = jwtTokenProvider.extractExpiration(refreshToken)
        assertNotNull(expiration)
        assertTrue(expiration.time > System.currentTimeMillis())
    }
    
    @Test
    fun `extractClaim 메서드는 토큰에서 특정 클레임을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetailsAdapter)
        
        // when
        val extractedUserId = jwtTokenProvider.extractClaim(token) { claims: Claims -> claims["userId"] as Int }
        
        // then
        assertEquals(userId.toInt(), extractedUserId)
    }
} 