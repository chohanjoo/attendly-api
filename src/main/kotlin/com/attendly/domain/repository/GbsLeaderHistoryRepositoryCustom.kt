package com.attendly.domain.repository

import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.User

interface GbsLeaderHistoryRepositoryCustom {
    fun findCurrentLeaderByGbsId(gbsId: Long): User?
    fun findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId: Long, leaderId: Long): GbsLeaderHistory?
    fun findCurrentLeaderByGbsGroupId(gbsGroupId: Long): GbsLeaderHistory?
    fun findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId: Long, leaderId: Long): GbsLeaderHistory?
    fun findByLeaderIdAndEndDateIsNull(leaderId: Long): GbsLeaderHistory?
} 