package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.Attendance
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AttendanceRepository : JpaRepository<Attendance, Long>, AttendanceRepositoryCustom {
    fun findByGbsGroupAndWeekStart(gbsGroup: GbsGroup, weekStart: LocalDate): List<Attendance>
    
    fun findByMemberAndWeekStartBetween(
        member: User,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Attendance>
} 