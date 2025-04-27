package com.church.attendly.service

import com.church.attendly.api.dto.SignupRequest
import com.church.attendly.api.dto.SignupResponse
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.repository.DepartmentRepository
import com.church.attendly.domain.repository.UserRepository
import com.church.attendly.exception.ResourceNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 회원가입 처리
     */
    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        // 이메일 중복 확인
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser.isPresent) {
            throw IllegalArgumentException("이미 사용 중인 이메일입니다")
        }

        // 부서 찾기
        val department = departmentRepository.findById(request.departmentId)
            .orElseThrow { ResourceNotFoundException("찾을 수 없는 부서입니다: ID ${request.departmentId}") }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        // 사용자 생성
        val user = User(
            name = request.name,
            email = request.email,
            password = encodedPassword,
            role = request.role,
            department = department
        )

        // 저장
        val savedUser = userRepository.save(user)

        // 응답 생성
        return SignupResponse(
            userId = savedUser.id ?: 0L,
            name = savedUser.name,
            email = savedUser.email ?: "",
            role = savedUser.role.name
        )
    }

    /**
     * 이메일로 사용자 찾기
     */
    fun findByEmail(email: String): Optional<User> {
        return userRepository.findByEmail(email)
    }

    /**
     * ID로 사용자 찾기
     */
    fun findById(id: Long): Optional<User> {
        return userRepository.findById(id)
    }
} 