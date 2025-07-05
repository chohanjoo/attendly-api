package com.attendly.domain.repository

import com.attendly.api.dto.LeaderGbsHistoryMemberDto
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
    
    // 특정 날짜에 활성화된 GBS 리더 히스토리를 찾는 메소드
    fun findCurrentLeaderHistoryByGbsId(gbsId: Long, date: LocalDate): GbsLeaderHistory?
    
    /**
     * 리더 히스토리와 해당 GBS의 멤버 정보를 한 번에 조회합니다.
     * N+1 문제를 방지하기 위해 조인 쿼리를 사용합니다.
     */
    fun findLeaderGbsHistoriesWithMembers(leaderId: Long): List<LeaderGbsHistoryMemberDto>
}