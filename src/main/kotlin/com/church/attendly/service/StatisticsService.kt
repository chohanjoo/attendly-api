package com.church.attendly.service

import com.church.attendly.api.dto.DepartmentStatistics
import com.church.attendly.api.dto.GbsStatistics
import com.church.attendly.api.dto.VillageStatistics
import com.church.attendly.api.dto.WeeklyStatistics
import com.church.attendly.domain.entity.WorshipStatus
import com.church.attendly.domain.repository.AttendanceRepository
import com.church.attendly.domain.repository.GbsGroupRepository
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.GbsMemberHistoryRepository
import com.church.attendly.exception.ResourceNotFoundException
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
class StatisticsService(
    private val attendanceRepository: AttendanceRepository,
    private val gbsGroupRepository: GbsGroupRepository,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository,
    private val organizationService: OrganizationService
) {

    /**
     * 부서 출석 통계 조회
     */
    @Transactional(readOnly = true)
    // @Cacheable("departmentStatistics")
    fun getDepartmentStatistics(departmentId: Long, startDate: LocalDate, endDate: LocalDate): DepartmentStatistics {
        val department = organizationService.getDepartmentById(departmentId)
        val villages = organizationService.getVillagesByDepartment(departmentId)
        
        if (villages.isEmpty()) {
            throw ResourceNotFoundException("부서에 마을이 없습니다: $departmentId")
        }
        
        // 마을별 통계 수집
        val villageStatsList = villages.map { village ->
            getVillageStatistics(village.id!!, startDate, endDate)
        }
        
        // 부서 전체 통계 계산
        val totalMembers = villageStatsList.sumOf { it.totalMembers }
        val attendedMembers = villageStatsList.sumOf { it.attendedMembers }
        val totalQtSum = villageStatsList.sumOf { it.averageQtCount * it.totalMembers }
        
        val attendanceRate = if (totalMembers > 0) {
            (attendedMembers.toDouble() / totalMembers) * 100
        } else {
            0.0
        }
        
        val averageQtCount = if (totalMembers > 0) {
            totalQtSum / totalMembers
        } else {
            0.0
        }
        
        return DepartmentStatistics(
            departmentId = department.id!!,
            departmentName = department.name,
            startDate = startDate,
            endDate = endDate,
            villageStats = villageStatsList,
            totalMembers = totalMembers,
            attendedMembers = attendedMembers,
            attendanceRate = attendanceRate,
            averageQtCount = averageQtCount
        )
    }
    
    /**
     * 마을 출석 통계 조회
     */
    @Transactional(readOnly = true)
    // @Cacheable("villageStatistics")
    fun getVillageStatistics(villageId: Long, startDate: LocalDate, endDate: LocalDate): VillageStatistics {
        val village = organizationService.getVillageById(villageId)
        val gbsGroups = organizationService.getActiveGbsGroupsByVillage(villageId)
        
        if (gbsGroups.isEmpty()) {
            throw ResourceNotFoundException("마을에 활성 GBS가 없습니다: $villageId")
        }
        
        // GBS별 통계 수집
        val gbsStatsList = gbsGroups.map { gbsGroup ->
            getGbsStatistics(gbsGroup.id!!, startDate, endDate)
        }
        
        // 마을 전체 통계 계산
        val totalMembers = gbsStatsList.sumOf { it.totalMembers }
        val attendedMembers = gbsStatsList.sumOf { it.attendedMembers }
        val totalQtSum = gbsStatsList.sumOf { it.averageQtCount * it.totalMembers }
        
        val attendanceRate = if (totalMembers > 0) {
            (attendedMembers.toDouble() / totalMembers) * 100
        } else {
            0.0
        }
        
        val averageQtCount = if (totalMembers > 0) {
            totalQtSum / totalMembers
        } else {
            0.0
        }
        
        return VillageStatistics(
            villageId = village.id!!,
            villageName = village.name,
            gbsStats = gbsStatsList,
            totalMembers = totalMembers,
            attendedMembers = attendedMembers,
            attendanceRate = attendanceRate,
            averageQtCount = averageQtCount
        )
    }
    
    /**
     * GBS 출석 통계 조회
     */
    @Transactional(readOnly = true)
    // @Cacheable("gbsStatistics")
    fun getGbsStatistics(gbsId: Long, startDate: LocalDate, endDate: LocalDate): GbsStatistics {
        val gbsGroup = organizationService.getGbsGroupById(gbsId)
        val leaderName = organizationService.getCurrentLeaderForGbs(gbsId)
        
        // 활성 멤버 수
        val totalMembers = gbsMemberHistoryRepository.countActiveMembers(gbsId, LocalDate.now()).toInt()
        
        // 주차별 통계 생성
        val weeklyStatsList = generateWeeklyDates(startDate, endDate).map { weekStart ->
            val attendances = attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, weekStart)
            
            val attendedMembers = attendances.count { it.worship == WorshipStatus.O }
            val attendanceRate = if (totalMembers > 0) {
                (attendedMembers.toDouble() / totalMembers) * 100
            } else {
                0.0
            }
            
            val qtSum = attendances.sumOf { it.qtCount }
            val averageQtCount = if (attendances.isNotEmpty()) {
                qtSum.toDouble() / attendances.size
            } else {
                0.0
            }
            
            WeeklyStatistics(
                weekStart = weekStart,
                totalMembers = totalMembers,
                attendedMembers = attendedMembers,
                attendanceRate = attendanceRate,
                averageQtCount = averageQtCount
            )
        }
        
        // GBS 전체 기간 통계 계산
        val attendedMembers = weeklyStatsList.sumOf { it.attendedMembers } / weeklyStatsList.size
        val attendanceRate = if (totalMembers > 0) {
            (attendedMembers.toDouble() / totalMembers) * 100
        } else {
            0.0
        }
        
        val averageQtCount = weeklyStatsList.map { it.averageQtCount }.average()
        
        return GbsStatistics(
            gbsId = gbsGroup.id!!,
            gbsName = gbsGroup.name,
            leaderName = leaderName,
            totalMembers = totalMembers,
            attendedMembers = attendedMembers,
            attendanceRate = attendanceRate,
            averageQtCount = averageQtCount,
            weeklyStats = weeklyStatsList
        )
    }
    
    /**
     * 시작일부터 종료일까지의 주차 시작일(일요일) 리스트 생성
     */
    private fun generateWeeklyDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        // 시작일의 이전 또는 같은 일요일을 계산
        var currentSunday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weeklyDates = mutableListOf<LocalDate>()
        
        // 시작일부터 종료일까지의 모든 일요일 수집
        while (!currentSunday.isAfter(endDate)) {
            weeklyDates.add(currentSunday)
            currentSunday = currentSunday.plusWeeks(1)
        }
        
        return weeklyDates
    }
} 