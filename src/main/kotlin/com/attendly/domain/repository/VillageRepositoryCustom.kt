package com.attendly.domain.repository

import com.attendly.domain.entity.Village
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface VillageRepositoryCustom {
    fun findVillageWithActiveGbsGroups(villageId: Long, date: LocalDate): Village?
    
    fun findVillagesWithParams(departmentId: Long?, name: String?, pageable: Pageable): Page<Village>
} 