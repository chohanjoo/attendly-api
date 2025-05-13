package com.attendly.domain.repository

import com.attendly.domain.entity.Attendance
import com.attendly.enums.AttendanceStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface AttendanceRepositoryCustom {
    fun countAttendancesByGbsIdsAndWeek(gbsIds: List<Long>, weekStart: LocalDate): Long
    
    fun findDetailsByGbsIdAndWeek(gbsId: Long, weekStart: LocalDate): List<Attendance>
    
    fun findDetailsByGbsIdAndWeeks(gbsId: Long, weekStarts: List<LocalDate>): Map<LocalDate, List<Attendance>>
    
    fun findByVillageIdAndWeek(villageId: Long, weekStart: LocalDate): List<Attendance>
    
    fun findAttendancesForAdmin(
        search: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        status: AttendanceStatus?,
        pageable: Pageable
    ): Page<Attendance>
} 