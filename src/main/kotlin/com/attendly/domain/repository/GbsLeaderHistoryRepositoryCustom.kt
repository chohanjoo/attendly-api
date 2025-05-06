package com.attendly.domain.repository

import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.User
import java.time.LocalDate

interface GbsLeaderHistoryRepositoryCustom {
    fun findCurrentLeaderByGbsId(gbsId: Long): User?
    fun findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId: Long, leaderId: Long): GbsLeaderHistory?
    fun findCurrentLeaderByGbsGroupId(gbsGroupId: Long): GbsLeaderHistory?
    fun findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId: Long, leaderId: Long): GbsLeaderHistory?
    fun findByLeaderIdAndEndDateIsNull(leaderId: Long): GbsLeaderHistory?
    
    // 특정 날짜에 활성화된 GBS 리더를 찾는 메소드 추가
    fun findLeaderByGbsIdAndDate(gbsId: Long, date: LocalDate): User?
} 