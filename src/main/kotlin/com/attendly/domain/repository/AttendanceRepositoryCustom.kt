package com.attendly.domain.repository

import com.attendly.domain.entity.Attendance
import java.time.LocalDate

interface AttendanceRepositoryCustom {
    fun countAttendancesByGbsIdsAndWeek(gbsIds: List<Long>, weekStart: LocalDate): Long
    
    fun findDetailsByGbsIdAndWeek(gbsId: Long, weekStart: LocalDate): List<Attendance>
    
    fun findDetailsByGbsIdAndWeeks(gbsId: Long, weekStarts: List<LocalDate>): Map<LocalDate, List<Attendance>>
    
    fun findByVillageIdAndWeek(villageId: Long, weekStart: LocalDate): List<Attendance>
} 