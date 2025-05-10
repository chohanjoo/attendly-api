package com.attendly.domain.repository

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsWithLeader
import java.time.LocalDate

interface GbsGroupRepositoryCustom {
    fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup>
    fun findByVillageAndTermDate(village: Village, startDate: LocalDate, endDate: LocalDate): List<GbsGroup>
    fun findWithCurrentLeader(gbsId: Long): GbsWithLeader?
} 