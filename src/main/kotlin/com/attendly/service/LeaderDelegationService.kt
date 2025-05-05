package com.attendly.service

import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.entity.User
import com.attendly.domain.entity.GbsGroup
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class LeaderDelegationService(
    private val leaderDelegationRepository: LeaderDelegationRepository,
    private val userRepository: UserRepository,
    private val gbsGroupRepository: GbsGroupRepository
) {

    @Transactional
    fun createDelegation(request: DelegationCreateRequest): LeaderDelegation {
        val delegator = userRepository.findById(request.delegatorId)
            .orElseThrow { AttendlyApiException(ErrorCode.USER_NOT_FOUND, "위임자를 찾을 수 없습니다") }
        
        val delegatee = userRepository.findById(request.delegateId)
            .orElseThrow { AttendlyApiException(ErrorCode.USER_NOT_FOUND, "수임자를 찾을 수 없습니다") }
        
        val gbsGroup = gbsGroupRepository.findById(request.gbsGroupId)
            .orElseThrow { AttendlyApiException(ErrorCode.RESOURCE_NOT_FOUND, "GBS 그룹을 찾을 수 없습니다") }

        validateDelegation(delegator, delegatee, gbsGroup, request.startDate, request.endDate)

        return leaderDelegationRepository.save(
            LeaderDelegation(
                delegator = delegator,
                delegatee = delegatee,
                gbsGroup = gbsGroup,
                startDate = request.startDate,
                endDate = request.endDate
            )
        )
    }

    @Transactional(readOnly = true)
    fun findActiveDelegations(userId: Long, date: LocalDate = LocalDate.now()): List<LeaderDelegation> {
        return leaderDelegationRepository.findActiveByDelegateIdAndDate(userId, date)
    }

    private fun validateDelegation(
        delegator: User,
        delegatee: User,
        gbsGroup: GbsGroup,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (startDate > endDate) {
            throw AttendlyApiException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 이전이거나 같아야 합니다")
        }
        
        if (startDate < LocalDate.now()) {
            throw AttendlyApiException(ErrorCode.INVALID_INPUT, "시작일은 현재 날짜 이후여야 합니다")
        }
        
        val existingDelegation = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate)
        if (existingDelegation != null) {
            throw AttendlyApiException(ErrorCode.DUPLICATE_RESOURCE, "이 GBS 그룹에 이미 활성 위임이 존재합니다")
        }
    }
}

data class DelegationCreateRequest(
    val delegatorId: Long,
    val delegateId: Long,
    val gbsGroupId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
) 