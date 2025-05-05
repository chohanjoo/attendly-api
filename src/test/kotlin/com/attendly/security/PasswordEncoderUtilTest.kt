package com.attendly.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

class PasswordEncoderUtilTest {

    private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

    @Test
    fun `'admin123' 비밀번호 암호화 및 검증 테스트`() {
        // 1. 주어진 평문 비밀번호
        val plainPassword = "admin123"

        // 2. 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(plainPassword)
        
        // 3. 결과 출력 (실제 값 확인용)
        println("원본 비밀번호: $plainPassword")
        println("암호화된 비밀번호: $encodedPassword")
        
        // 4. 검증: 암호화된 비밀번호는 매번 다른 값이 나오지만, 
        // 같은 평문 비밀번호로 검증하면 true가 나와야 함
        assertTrue(passwordEncoder.matches(plainPassword, encodedPassword))
        
        // 5. 다른 비밀번호로는 검증 실패해야 함
        assertFalse(passwordEncoder.matches("wrongpassword", encodedPassword))
    }

    @Test
    fun `여러번 암호화해도 서로 다른 해시값이 생성되지만 검증은 성공해야 함`() {
        // 1. 주어진 평문 비밀번호
        val plainPassword = "admin123"

        // 2. 여러 번 암호화
        val encodedPassword1 = passwordEncoder.encode(plainPassword)
        val encodedPassword2 = passwordEncoder.encode(plainPassword)
        val encodedPassword3 = passwordEncoder.encode(plainPassword)
        
        // 3. 결과 출력
        println("암호화된 비밀번호 1: $encodedPassword1")
        println("암호화된 비밀번호 2: $encodedPassword2")
        println("암호화된 비밀번호 3: $encodedPassword3")
        
        // 4. 각 암호화된 값은 서로 달라야 함 (솔트로 인해)
        assertNotEquals(encodedPassword1, encodedPassword2)
        assertNotEquals(encodedPassword2, encodedPassword3)
        assertNotEquals(encodedPassword1, encodedPassword3)
        
        // 5. 그러나 각각의 암호화된 값으로 검증하면 모두 성공해야 함
        assertTrue(passwordEncoder.matches(plainPassword, encodedPassword1))
        assertTrue(passwordEncoder.matches(plainPassword, encodedPassword2))
        assertTrue(passwordEncoder.matches(plainPassword, encodedPassword3))
    }
} 