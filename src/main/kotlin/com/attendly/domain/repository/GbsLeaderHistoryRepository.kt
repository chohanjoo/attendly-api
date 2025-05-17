package com.attendly.domain.repository

import com.attendly.domain.entity.GbsLeaderHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GbsLeaderHistoryRepository : JpaRepository<GbsLeaderHistory, Long>, GbsLeaderHistoryRepositoryCustom {
    fun findByLeaderIdOrderByStartDateDesc(leaderId: Long): List<GbsLeaderHistory>

    @Query(
        "SELECT h FROM GbsLeaderHistory h " +
                "JOIN FETCH h.gbsGroup g " +
                "JOIN FETCH g.village v " +
                "WHERE h.leader.id = :leaderId " +
                "ORDER BY h.startDate DESC"
    )
    fun findByLeaderIdWithDetailsOrderByStartDateDesc(@Param("leaderId") leaderId: Long): List<GbsLeaderHistory>

    override fun findByLeaderIdAndEndDateIsNull(leaderId: Long): GbsLeaderHistory?

    override @Query("SELECT glh FROM GbsLeaderHistory glh JOIN FETCH glh.leader WHERE glh.gbsGroup.id = :gbsId AND glh.leader.id = :leaderId")
    fun findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId: Long, leaderId: Long): GbsLeaderHistory?
}