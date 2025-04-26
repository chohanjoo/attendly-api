package com.church.attendly.security

import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var userDetails: UserDetails
    private lateinit var secretKey: String
    private val expirationTime = 3600000L // 1시간
    private val refreshExpirationTime = 86400000L // 24시간

    @BeforeEach
    fun setUp() {
        secretKey = "thisIsASecretKeyForTestingPurposesOnlyAndMustBeLongEnoughForHS256Algorithm"
        jwtTokenProvider = JwtTokenProvider(secretKey, expirationTime, refreshExpirationTime)
        
        // 테스트용 Department 생성
        val department = Department(
            id = 1L,
            name = "테스트 부서"
        )
        
        // 테스트용 User 생성
        val user = User(
            id = 1L,
            name = "테스트 사용자",
            birthDate = LocalDate.of(1990, 1, 1),
            role = Role.MEMBER,
            email = "test@example.com",
            password = "password",
            department = department
        )
        
        // UserDetailsAdapter 생성
        userDetails = UserDetailsAdapter(user)
    }

    @Test
    fun `generateToken 메서드는 유효한 토큰을 생성한다`() {
        // when
        val token = jwtTokenProvider.generateToken(userDetails)
        
        // then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }
    
    @Test
    fun `extractUsername 메서드는 토큰에서 사용자 이름을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetails)
        
        // when
        val username = jwtTokenProvider.extractUsername(token)
        
        // then
        assertEquals("test@example.com", username)
    }
    
    @Test
    fun `extractExpiration 메서드는 토큰에서 만료 시간을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetails)
        val expectedTime = Date(System.currentTimeMillis() + expirationTime)
        
        // when
        val expiration = jwtTokenProvider.extractExpiration(token)
        
        // then
        // 만료 시간은 근사치로 확인 (몇 초 차이가 날 수 있음)
        val timeDiff = Math.abs(expectedTime.time - expiration.time)
        assertTrue(timeDiff < 1000) // 1초 이내 차이
    }
    
    @Test
    fun `validateToken 메서드는 유효한 토큰에 대해 true를 반환한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetails)
        
        // when
        val isValid = jwtTokenProvider.validateToken(token, userDetails)
        
        // then
        assertTrue(isValid)
    }
    
    @Test
    fun `validateToken 메서드는 만료된 토큰에 대해 예외를 발생시킨다`() {
        // given
        // 1초 후에 만료되는 토큰 생성
        val tempProvider = JwtTokenProvider(secretKey, 1000, refreshExpirationTime)
        val token = tempProvider.generateToken(userDetails)
        
        // 토큰이 만료되도록 1.5초 대기
        Thread.sleep(1500)
        
        // when & then
        assertThrows(ExpiredJwtException::class.java) {
            jwtTokenProvider.extractUsername(token)
        }
    }
    
    @Test
    fun `validateToken 메서드는 다른 사용자의 토큰에 대해 false를 반환한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetails)
        
        // 다른 사용자 정보로 UserDetailsAdapter 생성
        val department = Department(id = 2L, name = "다른 부서")
        val otherUser = User(
            id = 2L,
            name = "다른 사용자",
            role = Role.MEMBER,
            email = "other@example.com",
            password = "password",
            department = department
        )
        val otherUserDetails = UserDetailsAdapter(otherUser)
        
        // when
        val isValid = jwtTokenProvider.validateToken(token, otherUserDetails)
        
        // then
        assertFalse(isValid)
    }
    
    @Test
    fun `generateRefreshToken 메서드는 유효한 리프레시 토큰을 생성한다`() {
        // when
        val refreshToken = jwtTokenProvider.generateRefreshToken(userDetails)
        
        // then
        assertNotNull(refreshToken)
        assertTrue(refreshToken.isNotEmpty())
        
        // 리프레시 토큰의 만료 시간이 액세스 토큰보다 긴지 확인
        val expiration = jwtTokenProvider.extractExpiration(refreshToken)
        val expectedTime = Date(System.currentTimeMillis() + refreshExpirationTime)
        val timeDiff = Math.abs(expectedTime.time - expiration.time)
        assertTrue(timeDiff < 1000) // 1초 이내 차이
    }
    
    @Test
    fun `extractClaim 메서드는 토큰에서 특정 클레임을 추출한다`() {
        // given
        val token = jwtTokenProvider.generateToken(userDetails)
        
        // when
        val userId = jwtTokenProvider.extractClaim(token) { claims: Claims -> claims["userId"] as Int }
        
        // then
        assertEquals(1, userId)
    }
} 