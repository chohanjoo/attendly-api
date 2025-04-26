package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.LeaderDelegation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface LeaderDelegationRepository : JpaRepository<LeaderDelegation, Long> {
    
    @Query("""
        SELECT d FROM LeaderDelegation d
        WHERE d.delegatee.id = :delegateeId
        AND d.gbsGroup.id = :gbsId
        AND d.startDate <= :date
        AND (d.endDate IS NULL OR d.endDate >= :date)
    """)
    fun findActiveDelegationsByDelegateeAndGbs(
        @Param("delegateeId") delegateeId: Long,
        @Param("gbsId") gbsId: Long,
        @Param("date") date: LocalDate
    ): List<LeaderDelegation>
} 