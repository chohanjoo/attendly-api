package com.attendly.service

import com.attendly.api.dto.VillageLeaderAssignRequest
import com.attendly.api.dto.VillageLeaderResponse
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminVillageLeaderService(
    private val villageLeaderRepository: VillageLeaderRepository,
    private val userRepository: UserRepository,
    private val villageRepository: VillageRepository
) {

    /**
     * 마을장 등록
     */
    @Transactional
    fun assignVillageLeader(request: VillageLeaderAssignRequest): VillageLeaderResponse {
        // 사용자와 마을 조회
        val user = userRepository.findById(request.userId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, request.userId)) }

        val village = villageRepository.findById(request.villageId)
            .orElseThrow { AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, request.villageId)) }

        // 현재 해당 마을에 마을장이 있는지 확인
        val currentLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(request.villageId)
        if (currentLeader != null) {
            // 현재 마을장의 임기 종료 처리
            val updatedLeader = VillageLeader(
                user = currentLeader.user,
                village = currentLeader.village,
                startDate = currentLeader.startDate,
                endDate = request.startDate.minusDays(1),
                createdAt = currentLeader.createdAt
            )
            villageLeaderRepository.save(updatedLeader)
        }

        // 현재 해당 사용자가 다른 마을의 마을장인지 확인
        val currentUserAsLeader = villageLeaderRepository.findByUserIdAndEndDateIsNull(request.userId)
        if (currentUserAsLeader != null) {
            // 해당 사용자의 다른 마을 마을장 임기 종료 처리
            val updatedLeader = VillageLeader(
                user = currentUserAsLeader.user,
                village = currentUserAsLeader.village,
                startDate = currentUserAsLeader.startDate,
                endDate = request.startDate.minusDays(1),
                createdAt = currentUserAsLeader.createdAt
            )
            villageLeaderRepository.save(updatedLeader)
        }

        // 새 마을장 등록
        val villageLeader = VillageLeader(
            user = user,
            village = village,
            startDate = request.startDate,
            endDate = request.endDate
        )

        val savedVillageLeader = villageLeaderRepository.save(villageLeader)

        return VillageLeaderResponse(
            userId = savedVillageLeader.user.id ?: 0L,
            userName = savedVillageLeader.user.name,
            villageId = savedVillageLeader.village.id ?: 0L,
            villageName = savedVillageLeader.village.name,
            startDate = savedVillageLeader.startDate,
            endDate = savedVillageLeader.endDate,
            createdAt = savedVillageLeader.createdAt,
            updatedAt = savedVillageLeader.updatedAt
        )
    }

    /**
     * 마을장 조회
     */
    fun getVillageLeader(villageId: Long): VillageLeaderResponse? {
        val villageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) ?: return null

        return VillageLeaderResponse(
            userId = villageLeader.user.id ?: 0L,
            userName = villageLeader.user.name,
            villageId = villageLeader.village.id ?: 0L,
            villageName = villageLeader.village.name,
            startDate = villageLeader.startDate,
            endDate = villageLeader.endDate,
            createdAt = villageLeader.createdAt,
            updatedAt = villageLeader.updatedAt
        )
    }

    /**
     * 마을장 종료
     */
    @Transactional
    fun terminateVillageLeader(villageId: Long, endDate: LocalDate): VillageLeaderResponse {
        val villageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId)
            ?: throw AttendlyApiException(ErrorMessage.VILLAGE_LEADER_NOT_FOUND, "해당 마을의 마을장이 존재하지 않습니다.")

        val updatedLeader = VillageLeader(
            user = villageLeader.user,
            village = villageLeader.village,
            startDate = villageLeader.startDate,
            endDate = endDate,
            createdAt = villageLeader.createdAt
        )

        val savedVillageLeader = villageLeaderRepository.save(updatedLeader)

        return VillageLeaderResponse(
            userId = savedVillageLeader.user.id ?: 0L,
            userName = savedVillageLeader.user.name,
            villageId = savedVillageLeader.village.id ?: 0L,
            villageName = savedVillageLeader.village.name,
            startDate = savedVillageLeader.startDate,
            endDate = savedVillageLeader.endDate,
            createdAt = savedVillageLeader.createdAt,
            updatedAt = savedVillageLeader.updatedAt
        )
    }
} 