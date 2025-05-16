package com.attendly.service

import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.admin.AdminAttendanceResponse
import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
import com.attendly.api.dto.admin.AdminAttendanceStatisticsResponse
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.enums.AttendanceStatus
import com.attendly.enums.MinistryStatus
import com.attendly.enums.WorshipStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Service
class AdminAttendanceService(
    private val attendanceRepository: AttendanceRepository
) {
    
    @Transactional(readOnly = true)
    fun getAttendancesForAdmin(request: AdminAttendanceSearchRequest): PageResponse<AdminAttendanceResponse> {
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "weekStart")
        )
        
        val attendancesPage = attendanceRepository.findAttendancesForAdmin(
            search = request.search,
            startDate = request.startDate,
            endDate = request.endDate,
            status = request.status,
            pageable = pageable
        )
        
        val attendanceResponses = attendancesPage.content.map { attendance ->
            AdminAttendanceResponse.fromEntity(
                id = attendance.id ?: -1,
                userId = attendance.member.id ?: -1,
                userName = attendance.member.name,
                date = attendance.weekStart,
                worship = attendance.worship,
                qtCount = attendance.qtCount,
                ministry = attendance.ministry
            )
        }
        
        return PageResponse(
            items = attendanceResponses,
            totalCount = attendancesPage.totalElements,
            hasMore = attendancesPage.number + 1 < attendancesPage.totalPages
        )
    }
    
    /**
     * 출석 통계 정보를 계산하여 반환
     */
    @Transactional(readOnly = true)
    fun getAttendanceStatistics(): AdminAttendanceStatisticsResponse {
        // 현재 월의 시작일과 종료일
        val now = LocalDate.now()
        val currentMonthStart = now.with(TemporalAdjusters.firstDayOfMonth())
        val currentMonthEnd = now.with(TemporalAdjusters.lastDayOfMonth())
        
        // 이전 월의 시작일과 종료일
        val prevMonth = now.minusMonths(1)
        val prevMonthStart = prevMonth.with(TemporalAdjusters.firstDayOfMonth())
        val prevMonthEnd = prevMonth.with(TemporalAdjusters.lastDayOfMonth())
        
        // 현재 월 출석 데이터 조회
        val currentMonthAttendances = attendanceRepository.findAttendancesForAdmin(
            search = null,
            startDate = currentMonthStart,
            endDate = currentMonthEnd,
            status = null,
            pageable = PageRequest.of(0, Int.MAX_VALUE)
        ).content
        
        // 이전 월 출석 데이터 조회
        val prevMonthAttendances = attendanceRepository.findAttendancesForAdmin(
            search = null,
            startDate = prevMonthStart,
            endDate = prevMonthEnd,
            status = null,
            pageable = PageRequest.of(0, Int.MAX_VALUE)
        ).content
        
        // 데이터 계산
        val totalCurrentCount = currentMonthAttendances.size.toDouble()
        val totalPrevCount = prevMonthAttendances.size.toDouble()
        
        // 현재 월 통계
        val currentAttendanceCount = currentMonthAttendances.count { 
            it.worship == WorshipStatus.O && it.ministry == MinistryStatus.A 
        }.toDouble()
        val currentLateCount = currentMonthAttendances.count { 
            it.worship == WorshipStatus.O && it.ministry == MinistryStatus.B 
        }.toDouble()
        val currentAbsentCount = totalCurrentCount - currentAttendanceCount - currentLateCount
        
        // 이전 월 통계
        val prevAttendanceCount = if (totalPrevCount > 0) {
            prevMonthAttendances.count { 
                it.worship == WorshipStatus.O && it.ministry == MinistryStatus.A 
            }.toDouble()
        } else 0.0
        val prevLateCount = if (totalPrevCount > 0) {
            prevMonthAttendances.count { 
                it.worship == WorshipStatus.O && it.ministry == MinistryStatus.B 
            }.toDouble()
        } else 0.0
        val prevAbsentCount = if (totalPrevCount > 0) {
            totalPrevCount - prevAttendanceCount - prevLateCount
        } else 0.0
        
        // 비율 계산
        val currentAttendanceRate = if (totalCurrentCount > 0) (currentAttendanceCount / totalCurrentCount) * 100 else 0.0
        val currentLateRate = if (totalCurrentCount > 0) (currentLateCount / totalCurrentCount) * 100 else 0.0
        val currentAbsentRate = if (totalCurrentCount > 0) (currentAbsentCount / totalCurrentCount) * 100 else 0.0
        
        val prevAttendanceRate = if (totalPrevCount > 0) (prevAttendanceCount / totalPrevCount) * 100 else 0.0
        val prevLateRate = if (totalPrevCount > 0) (prevLateCount / totalPrevCount) * 100 else 0.0
        val prevAbsentRate = if (totalPrevCount > 0) (prevAbsentCount / totalPrevCount) * 100 else 0.0
        
        // 전월 대비 차이 계산
        val attendanceRateDiff = currentAttendanceRate - prevAttendanceRate
        val lateRateDiff = currentLateRate - prevLateRate
        val absentRateDiff = currentAbsentRate - prevAbsentRate
        
        return AdminAttendanceStatisticsResponse(
            attendanceRate = currentAttendanceRate,
            attendanceRateDifference = attendanceRateDiff,
            totalAttendanceCount = currentAttendanceCount.toInt(),
            absentRate = currentAbsentRate,
            absentRateDifference = absentRateDiff,
            lateRate = currentLateRate,
            lateRateDifference = lateRateDiff
        )
    }
} 