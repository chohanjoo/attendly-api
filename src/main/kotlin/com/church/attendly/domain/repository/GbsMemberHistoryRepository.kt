package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsMemberHistoryRepository : JpaRepository<GbsMemberHistory, Long> {
    
    @Query("""
        SELECT COUNT(h) FROM GbsMemberHistory h
        WHERE h.gbsGroup.id = :gbsId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """)
    fun countActiveMembers(
        @Param("gbsId") gbsId: Long,
        @Param("date") date: LocalDate
    ): Long
    
    @Query("""
        SELECT h FROM GbsMemberHistory h
        JOIN FETCH h.member
        WHERE h.gbsGroup.id = :gbsId
        AND h.startDate <= :date
        AND (h.endDate IS NULL OR h.endDate >= :date)
    """)
    fun findActiveMembers(
        @Param("gbsId") gbsId: Long,
        @Param("date") date: LocalDate
    ): List<GbsMemberHistory>
} 
 