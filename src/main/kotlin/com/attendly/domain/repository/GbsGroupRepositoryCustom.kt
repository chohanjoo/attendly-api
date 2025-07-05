package com.attendly.domain.repository

import com.attendly.api.dto.AdminGbsGroupQueryDto
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsWithLeader
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface GbsGroupRepositoryCustom {
    fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup>
    fun findByVillageAndTermDate(village: Village, startDate: LocalDate, endDate: LocalDate): List<GbsGroup>
    fun findWithCurrentLeader(gbsId: Long): GbsWithLeader?
    fun findAllGbsGroupsWithDetails(pageable: Pageable): Page<GbsGroup>
    fun findAllGbsGroupsWithCompleteDetails(pageable: Pageable): Page<AdminGbsGroupQueryDto>
} 