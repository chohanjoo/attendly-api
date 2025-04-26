package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsLeaderHistoryRepository : JpaRepository<GbsLeaderHistory, Long> {
    
    fun findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId: Long, leaderId: Long): GbsLeaderHistory?
    
    @Query("""
        SELECT h.leader FROM GbsLeaderHistory h
        WHERE h.gbsGroup.id = :gbsId
        AND h.endDate IS NULL
    """)
    fun findCurrentLeaderByGbsId(@Param("gbsId") gbsId: Long): User?
} 