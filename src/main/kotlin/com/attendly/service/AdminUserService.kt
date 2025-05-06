package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.User
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val passwordEncoder: PasswordEncoder
) {

    /**
     * 사용자 생성
     */
    @Transactional
    fun createUser(request: UserCreateRequest): UserResponse {
        // 이메일 중복 확인
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser.isPresent) {
            throw AttendlyApiException(ErrorMessage.DUPLICATE_EMAIL)
        }

        // 부서 찾기
        val department = departmentRepository.findById(request.departmentId)
            .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)) }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        // 사용자 생성
        val user = User(
            name = request.name,
            email = request.email,
            phoneNumber = request.phoneNumber,
            password = encodedPassword,
            role = request.role,
            birthDate = request.birthDate,
            department = department
        )

        // 저장
        val savedUser = userRepository.save(user)

        // 응답 생성
        return UserResponse(
            id = savedUser.id ?: 0L,
            name = savedUser.name,
            email = savedUser.email,
            phoneNumber = savedUser.phoneNumber,
            role = savedUser.role,
            departmentId = savedUser.department.id ?: 0L,
            departmentName = savedUser.department.name,
            birthDate = savedUser.birthDate,
            createdAt = savedUser.createdAt,
            updatedAt = savedUser.updatedAt
        )
    }

    /**
     * 사용자 정보 수정
     */
    @Transactional
    fun updateUser(userId: Long, request: UserUpdateRequest): UserResponse {
        // 사용자 찾기
        val user = userRepository.findById(userId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId)) }

        // 이메일 변경 시 중복 확인
        if (request.email != null && request.email != user.email) {
            val existingUser = userRepository.findByEmail(request.email)
            if (existingUser.isPresent) {
                throw AttendlyApiException(ErrorMessage.DUPLICATE_EMAIL)
            }
        }

        // 부서 변경 시 확인
        val department = if (request.departmentId != null && request.departmentId != user.department.id) {
            departmentRepository.findById(request.departmentId)
                .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)) }
        } else {
            user.department
        }

        // 새 사용자 객체 생성 (불변성 유지)
        val updatedUser = User(
            id = user.id,
            name = request.name,
            email = request.email ?: user.email,
            phoneNumber = request.phoneNumber ?: user.phoneNumber,
            password = user.password,
            role = request.role ?: user.role,
            birthDate = request.birthDate ?: user.birthDate,
            department = department,
            createdAt = user.createdAt
        )

        // 저장
        val savedUser = userRepository.save(updatedUser)

        // 응답 생성
        return UserResponse(
            id = savedUser.id ?: 0L,
            name = savedUser.name,
            email = savedUser.email,
            phoneNumber = savedUser.phoneNumber,
            role = savedUser.role,
            departmentId = savedUser.department.id ?: 0L,
            departmentName = savedUser.department.name,
            birthDate = savedUser.birthDate,
            createdAt = savedUser.createdAt,
            updatedAt = savedUser.updatedAt
        )
    }

    /**
     * 비밀번호 초기화
     */
    @Transactional
    fun resetPassword(userId: Long, request: UserPasswordResetRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId)) }

        val encodedPassword = passwordEncoder.encode(request.newPassword)

        // 새 사용자 객체 생성 (불변성 유지)
        val updatedUser = User(
            id = user.id,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            password = encodedPassword,
            role = user.role,
            birthDate = user.birthDate,
            department = user.department,
            createdAt = user.createdAt
        )

        userRepository.save(updatedUser)
    }

    /**
     * 사용자 삭제
     */
    @Transactional
    fun deleteUser(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId))
        }
        userRepository.deleteById(userId)
    }

    /**
     * 사용자 조회
     */
    fun getUser(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId)) }

        return UserResponse(
            id = user.id ?: 0L,
            name = user.name,
            email = user.email,
            phoneNumber = user.phoneNumber,
            role = user.role,
            departmentId = user.department.id ?: 0L,
            departmentName = user.department.name,
            birthDate = user.birthDate,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }

    /**
     * 모든 사용자 조회
     */
    fun getAllUsers(pageable: Pageable): Page<UserResponse> {
        return userRepository.findAll(pageable).map { user ->
            UserResponse(
                id = user.id ?: 0L,
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
                role = user.role,
                departmentId = user.department.id ?: 0L,
                departmentName = user.department.name,
                birthDate = user.birthDate,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }

    /**
     * 사용자 일괄 등록
     */
    @Transactional
    fun bulkCreateUsers(request: UserBulkCreateRequest): UserBulkCreateResponse {
        val failedEmails = mutableListOf<String>()
        var createdCount = 0

        for (userRequest in request.users) {
            try {
                createUser(userRequest)
                createdCount++
            } catch (e: Exception) {
                failedEmails.add(userRequest.email)
            }
        }

        return UserBulkCreateResponse(
            createdCount = createdCount,
            failedCount = request.users.size - createdCount,
            failedEmails = failedEmails
        )
    }
} 