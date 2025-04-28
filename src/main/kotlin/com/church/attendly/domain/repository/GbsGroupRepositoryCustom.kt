package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsGroup
import java.time.LocalDate

interface GbsGroupRepositoryCustom {
    fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup>
} 