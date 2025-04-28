package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.Village
import java.time.LocalDate

interface GbsGroupRepositoryCustom {
    fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup>
    fun findByVillageAndTermDate(village: Village, startDate: LocalDate, endDate: LocalDate): List<GbsGroup>
} 