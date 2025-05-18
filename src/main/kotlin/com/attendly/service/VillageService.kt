package com.attendly.service

import com.attendly.api.dto.MemberInfo
import com.attendly.api.dto.UserVillageResponse
import com.attendly.domain.model.UserFilterDto
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import com.attendly.security.UserDetailsAdapter
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VillageService(
    private val villageRepository: VillageRepository,
    private val villageLeaderRepository: VillageLeaderRepository,
    private val userRepository: UserRepository
) {

    /**
     * 특정 마을 정보 조회
     *
     * @param villageId 마을 ID
     * @return 마을 정보 응답 객체
     */
    @Transactional(readOnly = true)
    fun getVillageById(villageId: Long, authentication: Authentication): UserVillageResponse {
        val village = villageRepository.findById(villageId)
            .orElseThrow {
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND,
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                )
            }

        // 현재 마을의 마을장 여부 확인
        val currentVillageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId)
        val isVillageLeader = if (currentVillageLeader != null) {
            val principal = authentication.principal as? UserDetailsAdapter
            principal?.getUser()?.id == currentVillageLeader.user.id
        } else {
            false
        }

        return UserVillageResponse(
            userId = 0L, // 특정 마을을 볼 때는 사용자 정보가 필요 없음
            userName = "", // 특정 마을을 볼 때는 사용자 정보가 필요 없음
            villageId = village.id!!,
            villageName = village.name,
            departmentId = village.department.id!!,
            departmentName = village.department.name,
            isVillageLeader = isVillageLeader
        )
    }

    /**
     * 특정 마을의 멤버 목록 조회
     *
     * @param villageId 마을 ID
     * @return 마을 멤버 목록 응답 객체
     */
    @Transactional(readOnly = true)
    fun getVillageMembers(villageId: Long): List<MemberInfo> {
        val village = villageRepository.findById(villageId)
            .orElseThrow {
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND,
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                )
            }

        val members = userRepository.findByFilters(
            UserFilterDto(
                villageId = villageId,
                roles = listOf(Role.MEMBER),
            )
        )

        return members.map { user ->
            MemberInfo(
                id = user.id!!,
                name = user.name,
                birthDate = user.birthDate,
                email = user.email,
                phoneNumber = user.phoneNumber,
                role = user.role.name,
                joinDate = null // 가입일은 현재 User 엔티티에 없음
            )
        }
    }
} 