package com.attendly.domain.repository

import com.attendly.domain.entity.LeaderDelegation
import java.time.LocalDate

interface LeaderDelegationRepositoryCustom {
    fun findActiveDelegationsByDelegateeAndGbs(delegateeId: Long, gbsId: Long, date: LocalDate): List<LeaderDelegation>
    
    fun findActiveByGbsGroupIdAndDate(gbsGroupId: Long, date: LocalDate): LeaderDelegation?
    
    fun findActiveByDelegateIdAndDate(userId: Long, date: LocalDate): List<LeaderDelegation>
    
    fun findActiveByGbsIdAndDelegateeId(gbsId: Long, delegateeId: Long, date: LocalDate): LeaderDelegation?
    
    fun findActiveByUserIdAndDate(userId: Long, date: LocalDate, isDelegator: Boolean): List<LeaderDelegation>
} 