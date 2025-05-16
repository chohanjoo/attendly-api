package com.attendly.service

import com.attendly.api.dto.minister.DepartmentStatisticsResponse
import com.attendly.api.dto.minister.MemberStatisticsItem
import com.attendly.api.dto.minister.VillageDetailStatisticsResponse
import com.attendly.api.dto.minister.VillageStatisticsItem
import com.attendly.api.dto.minister.VillageWeeklyStatisticsItem
import com.attendly.api.dto.minister.WeeklyStatisticsItem
import com.attendly.domain.entity.Department
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
class MinisterStatisticsService(
    private val attendanceRepository: AttendanceRepository,
    private val departmentRepository: DepartmentRepository,
    private val villageRepository: VillageRepository,
    private val userRepository: UserRepository,
    private val organizationService: OrganizationService,
    private val statisticsService: StatisticsService
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    /**
     * 부서 통계 요약 조회
     * 특정 부서의 통계 요약 정보를 제공합니다.
     * 
     * @param departmentId 부서 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 부서 통계 요약 정보
     */
    @Transactional(readOnly = true)
    fun getDepartmentStatistics(departmentId: Long, startDate: LocalDate, endDate: LocalDate): DepartmentStatisticsResponse {
        // 부서 조회
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.DEPARTMENT_NOT_FOUND,
                    ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId)
                )
            }
        
        // 해당 부서의 마을 목록 조회
        val villages = villageRepository.findByDepartment(department)
        
        if (villages.isEmpty()) {
            throw AttendlyApiException(
                ErrorMessage.VILLAGE_NOT_FOUND,
                "부서에 소속된 마을이 없습니다."
            )
        }
        
        log.debug("getDepartmentStatistics; villages: {}", villages)
        
        // 부서에 속한 전체 멤버 수
        val departmentMembers = userRepository.countByDepartment(department)
        
        // 주차별 시작일 계산 (일요일 기준)
        val weekStarts = getWeekStartsInRange(startDate, endDate)
        
        // 마을별 통계 수집
        val villageStats = villages.map { village ->
            val villageStatistics = statisticsService.getVillageStatistics(village.id!!, startDate, endDate)
            
            VillageStatisticsItem(
                villageId = village.id!!,
                villageName = village.name,
                totalMembers = villageStatistics.totalMembers,
                attendedMembers = villageStatistics.attendedMembers,
                attendanceRate = villageStatistics.attendanceRate,
                averageQtCount = villageStatistics.averageQtCount
            )
        }
        
        // 부서 전체 통계 계산
        val totalMembers = villageStats.sumOf { it.totalMembers }
        val attendedMembers = villageStats.sumOf { it.attendedMembers }
        
        val attendanceRate = if (totalMembers > 0) {
            (attendedMembers.toDouble() / totalMembers) * 100
        } else {
            0.0
        }
        
        val totalQtSum = villageStats.sumOf { it.averageQtCount * it.totalMembers }
        val averageQtCount = if (totalMembers > 0) {
            totalQtSum / totalMembers
        } else {
            0.0
        }
        
        // 주간 통계 계산
        val weeklyStats = weekStarts.map { weekStart ->
            val weekEnd = weekStart.plusDays(6)
            val attendancesForWeek = villages.flatMap { village ->
                attendanceRepository.findByVillageIdAndWeek(village.id!!, weekStart)
            }
            
            val weekAttendedMembers = attendancesForWeek.size
            val weekAttendanceRate = if (totalMembers > 0) {
                (weekAttendedMembers.toDouble() / totalMembers) * 100
            } else {
                0.0
            }
            
            WeeklyStatisticsItem(
                weekStart = weekStart,
                totalMembers = totalMembers,
                attendedMembers = weekAttendedMembers,
                attendanceRate = weekAttendanceRate
            )
        }
        
        return DepartmentStatisticsResponse(
            departmentId = department.id!!,
            departmentName = department.name,
            totalMembers = totalMembers,
            attendedMembers = attendedMembers,
            attendanceRate = attendanceRate,
            averageQtCount = averageQtCount,
            villages = villageStats,
            weeklyStats = weeklyStats
        )
    }
    
    /**
     * 마을별 상세 통계 조회
     * 특정 마을의 상세 통계 정보를 제공합니다.
     * 
     * @param departmentId 부서 ID
     * @param villageId 마을 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 마을 상세 통계 정보
     */
    @Transactional(readOnly = true)
    fun getVillageDetailStatistics(departmentId: Long, villageId: Long, startDate: LocalDate, endDate: LocalDate): VillageDetailStatisticsResponse {
        // 부서 조회
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.DEPARTMENT_NOT_FOUND,
                    ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId)
                )
            }
        
        // 마을 조회
        val village = villageRepository.findById(villageId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND,
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                )
            }
        
        // 마을이 해당 부서에 속하는지 확인
        if (village.department.id != departmentId) {
            throw AttendlyApiException(
                ErrorMessage.VILLAGE_NOT_IN_DEPARTMENT,
                "마을이 해당 부서에 속하지 않습니다."
            )
        }
        
        log.debug("getVillageDetailStatistics; village: {}", village)
        
        // 마을 통계 조회
        val villageStatistics = statisticsService.getVillageStatistics(villageId, startDate, endDate)
        
        // 마을에 속한 모든 멤버 조회
        val members = userRepository.findByVillage(village)
        log.debug("getVillageDetailStatistics; members: {}", members)
        
        // 주차별 시작일 계산 (일요일 기준)
        val weekStarts = getWeekStartsInRange(startDate, endDate)
        
        // 기간 내 출석 기록 모두 조회
        val attendances = attendanceRepository.findByVillageIdAndDateRange(villageId, startDate, endDate)
        log.debug("getVillageDetailStatistics; attendances size: {}", attendances.size)
        
        // 멤버별 통계 계산
        val memberStats = members.map { member ->
            val memberAttendances = attendances.filter { it.member.id == member.id }
            val totalWeeks = weekStarts.size
            
            val attendanceCount = memberAttendances.size
            val attendanceRate = if (totalWeeks > 0) {
                (attendanceCount.toDouble() / totalWeeks) * 100
            } else {
                0.0
            }
            
            val qtCount = memberAttendances.sumOf { it.qtCount }
            
            MemberStatisticsItem(
                userId = member.id!!,
                userName = member.name,
                attendanceCount = attendanceCount,
                attendanceRate = attendanceRate,
                qtCount = qtCount
            )
        }
        
        // 주간 통계 계산
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val weeklyStats = weekStarts.map { weekStart ->
            val weekAttendances = attendances.filter { 
                val attendanceDate = it.weekStart
                !attendanceDate.isBefore(weekStart) && !attendanceDate.isAfter(weekStart.plusDays(6))
            }
            
            val totalMembers = members.size
            val attendedMembers = weekAttendances.map { it.member.id }.distinct().size
            val attendanceRate = if (totalMembers > 0) {
                (attendedMembers.toDouble() / totalMembers) * 100
            } else {
                0.0
            }
            
            VillageWeeklyStatisticsItem(
                weekStart = weekStart.format(formatter),
                totalMembers = totalMembers,
                attendedMembers = attendedMembers,
                attendanceRate = attendanceRate
            )
        }
        
        return VillageDetailStatisticsResponse(
            villageId = village.id!!,
            villageName = village.name,
            totalMembers = villageStatistics.totalMembers,
            attendedMembers = villageStatistics.attendedMembers,
            attendanceRate = villageStatistics.attendanceRate,
            averageQtCount = villageStatistics.averageQtCount,
            members = memberStats,
            weeklyStats = weeklyStats
        )
    }
    
    /**
     * 주어진 날짜 범위 내의 모든 주 시작일(일요일)을 반환합니다.
     */
    private fun getWeekStartsInRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        
        // startDate를 포함하는 주의 첫 번째 날(일요일)로 조정
        var currentWeekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        
        // endDate까지 모든 주의 시작일을 추가
        while (!currentWeekStart.isAfter(endDate)) {
            result.add(currentWeekStart)
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }
        
        return result
    }
} 