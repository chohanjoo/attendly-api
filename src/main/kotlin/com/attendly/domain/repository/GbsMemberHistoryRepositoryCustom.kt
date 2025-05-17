package com.attendly.domain.repository

import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import java.time.LocalDate

interface GbsMemberHistoryRepositoryCustom {
    fun countActiveMembers(gbsId: Long, date: LocalDate): Long
    fun findActiveMembers(condition: GbsMemberHistorySearchCondition): List<GbsMemberHistory>
    fun findCurrentMemberHistoryByMemberId(memberId: Long): GbsMemberHistory?
    fun findCurrentMembersByGbsId(gbsId: Long, date: LocalDate): List<GbsMemberHistory>
} 