package com.church.attendly.api.dto

import com.church.attendly.domain.entity.LeaderDelegation
import java.time.LocalDate

data class LeaderDelegationResponse(
    val id: Long,
    val delegatorId: Long,
    val delegatorName: String,
    val delegateeId: Long,
    val delegateeName: String,
    val gbsGroupId: Long,
    val gbsGroupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
) {
    companion object {
        fun from(delegation: LeaderDelegation): LeaderDelegationResponse {
            return LeaderDelegationResponse(
                id = delegation.id!!,
                delegatorId = delegation.delegator.id!!,
                delegatorName = delegation.delegator.name,
                delegateeId = delegation.delegatee.id!!,
                delegateeName = delegation.delegatee.name,
                gbsGroupId = delegation.gbsGroup.id!!,
                gbsGroupName = delegation.gbsGroup.name,
                startDate = delegation.startDate,
                endDate = delegation.endDate
            )
        }
    }
} 