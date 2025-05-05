package com.attendly.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Date
import java.util.function.Function
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${spring.security.jwt.secret-key}")
    private val secretString: String,
    
    @Value("\${spring.security.jwt.expiration-time}")
    private val expirationTime: Long,
    
    @Value("\${spring.security.jwt.refresh-expiration-time}")
    private val refreshExpirationTime: Long
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray())
    
    // 토큰에서 사용자 이름 추출
    fun extractUsername(token: String): String {
        return extractClaim(token, Claims::getSubject)
    }
    
    // 토큰에서 만료 시간 추출
    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }
    
    // 토큰에서 클레임 추출
    fun <T> extractClaim(token: String, claimsResolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return claimsResolver.apply(claims)
    }
    
    // 모든 클레임 추출
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
    }
    
    // 토큰 만료 여부 확인
    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }
    
    // Access 토큰 생성
    fun generateToken(userDetails: UserDetails): String {
        val claims = mutableMapOf<String, Any>()
        if (userDetails is UserDetailsAdapter) {
            claims["userId"] = userDetails.getId() ?: 0
        }
        return createToken(claims, userDetails.username, expirationTime)
    }
    
    // Refresh 토큰 생성
    fun generateRefreshToken(userDetails: UserDetails): String {
        val claims = mutableMapOf<String, Any>()
        if (userDetails is UserDetailsAdapter) {
            claims["userId"] = userDetails.getId() ?: 0
        }
        return createToken(claims, userDetails.username, refreshExpirationTime)
    }
    
    // 토큰 생성 공통 메서드
    private fun createToken(claims: Map<String, Any>, subject: String, expiration: Long): String {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact()
    }
    
    // 토큰 유효성 검증
    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return (username == userDetails.username && !isTokenExpired(token))
    }
} 