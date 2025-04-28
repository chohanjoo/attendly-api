package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import java.time.LocalDate

interface GbsMemberHistoryRepositoryCustom {
    fun countActiveMembers(gbsId: Long, date: LocalDate): Long
    fun findActiveMembers(gbsId: Long, date: LocalDate): List<GbsMemberHistory>
} 