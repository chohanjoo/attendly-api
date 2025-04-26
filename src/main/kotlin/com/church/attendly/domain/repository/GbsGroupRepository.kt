package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.Village
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsGroupRepository : JpaRepository<GbsGroup, Long> {
    fun findByVillage(village: Village): List<GbsGroup>
    
    fun findByVillageAndTermStartDateLessThanEqualAndTermEndDateGreaterThanEqual(
        village: Village,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<GbsGroup>
    
    @Query("""
        SELECT g FROM GbsGroup g
        JOIN FETCH g.village v
        WHERE v.id = :villageId
        AND g.termStartDate <= :date
        AND g.termEndDate >= :date
    """)
    fun findActiveGroupsByVillageId(
        @Param("villageId") villageId: Long,
        @Param("date") date: LocalDate
    ): List<GbsGroup>
} 