package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsLeaderHistoryRepository : JpaRepository<GbsLeaderHistory, Long>, GbsLeaderHistoryRepositoryCustom {
    fun findByLeaderIdOrderByStartDateDesc(leaderId: Long): List<GbsLeaderHistory>
    
    @Query("SELECT h FROM GbsLeaderHistory h " +
           "JOIN FETCH h.gbsGroup g " +
           "JOIN FETCH g.village v " +
           "WHERE h.leader.id = :leaderId " +
           "ORDER BY h.startDate DESC")
    fun findByLeaderIdWithDetailsOrderByStartDateDesc(@Param("leaderId") leaderId: Long): List<GbsLeaderHistory>
} 