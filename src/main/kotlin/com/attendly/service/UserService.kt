package com.attendly.service

import com.attendly.api.dto.SignupRequest
import com.attendly.api.dto.SignupResponse
import com.attendly.api.dto.UserResponse
import com.attendly.api.dto.UserListByRolesRequest
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.exception.ResourceNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
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

    /**
     * 현재 인증된 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    fun getCurrentUser(authentication: Authentication): User {
        val email = authentication.name
        return userRepository.findByEmail(email)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다.") }
    }
    
    /**
     * 역할 목록으로 사용자 조회
     */
    @Transactional(readOnly = true)
    fun getUsersByRoles(request: UserListByRolesRequest): List<UserResponse> {
        if (request.roles.isEmpty()) {
            return emptyList()
        }
        
        val roles = request.roles.map { roleStr -> Role.valueOf(roleStr) }
        val users = roles.flatMap { role -> userRepository.findByRole(role) }.distinct()
        
        return users.map { user ->
            UserResponse(
                id = user.id ?: 0L,
                name = user.name,
                email = user.email,
                role = user.role,
                departmentId = user.department.id ?: 0L,
                departmentName = user.department.name,
                birthDate = user.birthDate,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
} 