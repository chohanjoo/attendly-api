package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.LeaderDelegation
import java.time.LocalDate

interface LeaderDelegationRepositoryCustom {
    fun findActiveDelegationsByDelegateeAndGbs(delegateeId: Long, gbsId: Long, date: LocalDate): List<LeaderDelegation>
    
    fun findActiveByGbsGroupIdAndDate(gbsGroupId: Long, date: LocalDate): LeaderDelegation?
    
    fun findActiveByDelegateIdAndDate(userId: Long, date: LocalDate): List<LeaderDelegation>
} 