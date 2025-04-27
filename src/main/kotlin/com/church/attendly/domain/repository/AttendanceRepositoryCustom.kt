package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.Attendance
import com.church.attendly.domain.entity.WorshipStatus
import java.time.LocalDate

interface AttendanceRepositoryCustom {
    fun countAttendancesByGbsIdsAndWeek(gbsIds: List<Long>, weekStart: LocalDate): Long
    
    fun findDetailsByGbsIdAndWeek(gbsId: Long, weekStart: LocalDate): List<Attendance>
    
    fun findByVillageIdAndWeek(villageId: Long, weekStart: LocalDate): List<Attendance>
} 