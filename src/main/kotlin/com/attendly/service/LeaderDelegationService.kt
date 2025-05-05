package com.attendly.service

import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.entity.User
import com.attendly.domain.entity.GbsGroup
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
            .orElseThrow { IllegalArgumentException("Delegator not found") }
        
        val delegatee = userRepository.findById(request.delegateId)
            .orElseThrow { IllegalArgumentException("Delegate not found") }
        
        val gbsGroup = gbsGroupRepository.findById(request.gbsGroupId)
            .orElseThrow { IllegalArgumentException("GBS Group not found") }

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
        require(startDate <= endDate) { "Start date must be before or equal to end date" }
        require(startDate >= LocalDate.now()) { "Start date must be in the future" }
        
        val existingDelegation = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate)
        require(existingDelegation == null) { "There is already an active delegation for this GBS group" }
    }
}

data class DelegationCreateRequest(
    val delegatorId: Long,
    val delegateId: Long,
    val gbsGroupId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
) 