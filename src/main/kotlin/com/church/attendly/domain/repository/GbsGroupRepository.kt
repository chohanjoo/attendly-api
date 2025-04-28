package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.Village
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsGroupRepository : JpaRepository<GbsGroup, Long>, GbsGroupRepositoryCustom {
    fun findByVillage(village: Village): List<GbsGroup>
    
    fun findByVillageAndTermStartDateLessThanEqualAndTermEndDateGreaterThanEqual(
        village: Village,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<GbsGroup>
} 