package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import com.church.attendly.domain.model.GbsMemberHistorySearchCondition
import java.time.LocalDate

interface GbsMemberHistoryRepositoryCustom {
    fun countActiveMembers(gbsId: Long, date: LocalDate): Long
    fun findActiveMembers(condition: GbsMemberHistorySearchCondition): List<GbsMemberHistory>
} 