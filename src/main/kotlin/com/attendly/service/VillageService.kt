package com.attendly.service

import com.attendly.api.dto.UserVillageResponse
import com.attendly.domain.repository.VillageRepository
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import com.attendly.security.UserDetailsAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.security.core.Authentication

@Service
class VillageService(
    private val villageRepository: VillageRepository,
    private val villageLeaderRepository: VillageLeaderRepository
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
            .orElseThrow { AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)) }
        
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
} 