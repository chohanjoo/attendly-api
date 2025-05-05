package com.attendly.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 암호화된 비밀번호를 확인하기 위한 유틸리티 메인 클래스
 * 실행 방법: ./gradlew -PmainClass=com.attendly.security.PasswordEncoderUtilMain execute
 */
object PasswordEncoderUtilMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
        val plainPassword = "admin123"

        // 여러 번 암호화하여 결과 확인
        println("===== 비밀번호 암호화 결과 =====")
        println("원본 비밀번호: $plainPassword")
        
        // 여러 번 암호화하여 다른 결과가 나오는지 확인
        val encodedPassword1 = passwordEncoder.encode(plainPassword)
        val encodedPassword2 = passwordEncoder.encode(plainPassword)
        val encodedPassword3 = passwordEncoder.encode(plainPassword)
        
        println("암호화된 비밀번호 1: $encodedPassword1")
        println("암호화된 비밀번호 2: $encodedPassword2")
        println("암호화된 비밀번호 3: $encodedPassword3")

        // 검증 결과 확인
        println("\n===== 비밀번호 검증 결과 =====")
        println("원본 비밀번호 검증 결과: ${passwordEncoder.matches(plainPassword, encodedPassword1)}")
        println("잘못된 비밀번호 검증 결과: ${passwordEncoder.matches("wrongpassword", encodedPassword1)}")
    }
} 