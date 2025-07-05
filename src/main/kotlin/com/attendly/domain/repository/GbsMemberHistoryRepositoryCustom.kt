package com.attendly.domain.repository

import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import java.time.LocalDate

interface GbsMemberHistoryRepositoryCustom {
    fun countActiveMembers(gbsId: Long, date: LocalDate): Long
    fun findActiveMembers(condition: GbsMemberHistorySearchCondition): List<GbsMemberHistory>
    fun findCurrentMemberHistoryByMemberId(memberId: Long): GbsMemberHistory?
    fun findCurrentMembersByGbsId(gbsId: Long, date: LocalDate): List<GbsMemberHistory>
    
    /**
     * 마을의 모든 활성 GBS 멤버들을 한 번에 조회합니다.
     * N+1 문제를 방지하기 위해 IN 쿼리를 사용합니다.
     */
    fun findActiveMembersByVillageGbsIds(gbsIds: List<Long>, date: LocalDate): List<GbsMemberHistory>
} 