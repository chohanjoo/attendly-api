package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.Attendance
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findByGbsGroupAndWeekStart(gbsGroup: GbsGroup, weekStart: LocalDate): List<Attendance>
    
    fun findByMemberAndWeekStartBetween(
        member: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Attendance>
    
    @Query("""
        SELECT COUNT(a) FROM Attendance a
        WHERE a.gbsGroup.id IN :gbsIds
        AND a.weekStart = :weekStart
        AND a.worship = 'O'
    """)
    fun countAttendancesByGbsIdsAndWeek(
        @Param("gbsIds") gbsIds: List<Long>,
        @Param("weekStart") weekStart: LocalDate
    ): Long
    
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.member m
        WHERE a.gbsGroup.id = :gbsId
        AND a.weekStart = :weekStart
    """)
    fun findDetailsByGbsIdAndWeek(
        @Param("gbsId") gbsId: Long,
        @Param("weekStart") weekStart: LocalDate
    ): List<Attendance>
    
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.member m
        JOIN FETCH a.gbsGroup g
        WHERE g.village.id = :villageId
        AND a.weekStart = :weekStart
    """)
    fun findByVillageIdAndWeek(
        @Param("villageId") villageId: Long,
        @Param("weekStart") weekStart: LocalDate
    ): List<Attendance>
} 