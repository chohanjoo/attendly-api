package com.attendly.domain.repository

import com.attendly.domain.entity.Village
import java.time.LocalDate

interface VillageRepositoryCustom {
    fun findVillageWithActiveGbsGroups(villageId: Long, date: LocalDate): Village?
} 