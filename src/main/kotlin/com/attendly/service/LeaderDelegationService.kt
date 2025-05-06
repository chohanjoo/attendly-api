package com.attendly.service

import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.entity.User
import com.attendly.domain.entity.GbsGroup
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
import com.attendly.exception.ErrorMessage
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
            .orElseThrow { AttendlyApiException(ErrorMessage.DELEGATOR_NOT_FOUND) }
        
        val delegatee = userRepository.findById(request.delegateId)
            .orElseThrow { AttendlyApiException(ErrorMessage.DELEGATEE_NOT_FOUND) }
        
        val gbsGroup = gbsGroupRepository.findById(request.gbsGroupId)
            .orElseThrow { AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND) }

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
        // 수임자로서의 권한 위임 이력
        val asDelegatee = leaderDelegationRepository.findActiveByDelegateIdAndDate(userId, date)
        
        // 위임자로서의 권한 위임 이력
        val asDelegator = leaderDelegationRepository.findActiveByUserIdAndDate(userId, date, true)
        
        // 두 결과를 합쳐서 반환
        return asDelegatee + asDelegator
    }

    private fun validateDelegation(
        delegator: User,
        delegatee: User,
        gbsGroup: GbsGroup,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (startDate > endDate) {
            throw AttendlyApiException(ErrorMessage.INVALID_DELEGATION_DATES)
        }
        
        if (startDate < LocalDate.now()) {
            throw AttendlyApiException(ErrorMessage.INVALID_START_DATE)
        }
        
        val existingDelegation = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate)
        if (existingDelegation != null) {
            throw AttendlyApiException(ErrorMessage.DUPLICATE_DELEGATION)
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