package com.attendly.service

import com.attendly.api.dto.SignupRequest
import com.attendly.api.dto.SignupResponse
import com.attendly.api.dto.UserResponse
import com.attendly.api.dto.UserListByRolesRequest
import com.attendly.api.dto.UserVillageResponse
import com.attendly.enums.Role
import com.attendly.domain.entity.User
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository,
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
            throw AttendlyApiException(ErrorMessage.DUPLICATE_EMAIL)
        }

        // 부서 찾기
        val department = departmentRepository.findById(request.departmentId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.DEPARTMENT_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)
                ) 
            }

        // 비밀번호 암호화
        val encodedPassword = passwordEncoder.encode(request.password)

        // 사용자 생성
        val user = User(
            name = request.name,
            email = request.email,
            phoneNumber = request.phoneNumber,
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
            phoneNumber = savedUser.phoneNumber,
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
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND) }
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
     * 사용자가 속한 마을 정보 조회
     */
    @Transactional(readOnly = true)
    fun getUserVillage(userId: Long): UserVillageResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId)) }
        
        // 마을장인 경우 직접 마을 정보 제공
        if (user.role == Role.VILLAGE_LEADER && user.villageLeader != null) {
            val village = user.villageLeader.village
            return UserVillageResponse(
                userId = user.id!!,
                userName = user.name,
                villageId = village.id!!,
                villageName = village.name,
                departmentId = village.department.id!!,
                departmentName = village.department.name,
                isVillageLeader = true
            )
        }
        
        // 일반 멤버인 경우 현재 속한 GBS를 통해 마을 정보 조회
        val currentMemberHistory = gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId)
        if (currentMemberHistory != null) {
            val village = currentMemberHistory.gbsGroup.village
            return UserVillageResponse(
                userId = user.id!!,
                userName = user.name,
                villageId = village.id!!,
                villageName = village.name,
                departmentId = village.department.id!!,
                departmentName = village.department.name,
                isVillageLeader = false
            )
        }
        
        // 마을에 속하지 않은 경우 예외 처리
        throw AttendlyApiException(ErrorMessage.USER_NOT_ASSIGNED_TO_VILLAGE)
    }

    /**
     * 현재 인증된 사용자의 마을 정보 조회
     */
    @Transactional(readOnly = true)
    fun getCurrentUserVillage(authentication: Authentication): UserVillageResponse {
        val user = getCurrentUser(authentication)
        return getUserVillage(user.id!!)
    }
} 