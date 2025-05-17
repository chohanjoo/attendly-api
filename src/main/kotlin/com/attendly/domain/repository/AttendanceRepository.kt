package com.attendly.domain.repository

import com.attendly.domain.entity.Attendance
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AttendanceRepository : JpaRepository<Attendance, Long>, AttendanceRepositoryCustom {
    fun findAllByGbsGroupAndWeekStart(gbsGroup: GbsGroup, weekStart: LocalDate): List<Attendance>
    
    fun findAllByMemberAndWeekStartBetween(
        member: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Attendance>
} 