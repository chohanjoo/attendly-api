package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.User

interface GbsLeaderHistoryRepositoryCustom {
    fun findCurrentLeaderByGbsId(gbsId: Long): User?
    fun findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId: Long, leaderId: Long): GbsLeaderHistory?
} 