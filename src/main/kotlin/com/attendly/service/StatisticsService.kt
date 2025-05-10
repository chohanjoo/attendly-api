package com.attendly.service

import com.attendly.api.dto.DepartmentStatistics
import com.attendly.api.dto.GbsStatistics
import com.attendly.api.dto.VillageStatistics
import com.attendly.api.dto.WeeklyStatistics
import com.attendly.domain.entity.WorshipStatus
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(this.javaClass)!!

    /**
     * 부서 출석 통계 조회
     */
    @Transactional(readOnly = true)
    fun getDepartmentStatistics(departmentId: Long, startDate: LocalDate, endDate: LocalDate): DepartmentStatistics {
        val department = organizationService.getDepartmentById(departmentId)
        val villages = organizationService.getVillagesByDepartment(departmentId)
        
        if (villages.isEmpty()) {
            throw AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND)
        }

        log.debug("getDepartmentStatistics;Villages: {}", villages)

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
    fun getVillageStatistics(villageId: Long, startDate: LocalDate, endDate: LocalDate): VillageStatistics {
        // 조인을 사용하여 마을과 활성 GBS 그룹을 한 번에 조회
        val (village, gbsGroups) = organizationService.getVillageWithActiveGbsGroups(villageId, startDate)

        log.debug("getVillageStatistics;gbsGroups: {}", gbsGroups)

        if (gbsGroups.isEmpty()) {
            throw AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND)
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
    fun getGbsStatistics(gbsId: Long, startDate: LocalDate, endDate: LocalDate): GbsStatistics {
        // 한 번의 조인 쿼리로 GBS 그룹과 리더 정보를 함께 조회
        val (gbsGroup, leaderNameOrNull) = organizationService.getGbsWithLeader(gbsId)
        // 리더 이름이 없는 경우 "리더 없음"으로 표시
        val leaderName = leaderNameOrNull ?: "리더 없음"
        
        // 활성 멤버 수 - 통계 기간의 시작일자 기준으로 조회하도록 수정
        val totalMembers = gbsMemberHistoryRepository.countActiveMembers(gbsId, startDate).toInt()

        log.debug("getGbsStatistics;gbsGroup: {}, totalMembers: {}", gbsGroup, totalMembers)

        if (totalMembers == 0) {
            throw AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND)
        }
        
        // 주차별 통계 생성
        val weeklyStatsList = generateWeeklyDates(startDate, endDate).map { weekStart ->
            val attendances = attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, weekStart)

            log.debug("getGbsStatistics;weekStart: {}, attendances: {}", weekStart, attendances)

            // 수정: GBS 출석 여부를 worship 상태가 아닌 attendance 테이블 기록 여부로 판단
            val attendedMembers = attendances.size
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
        val attendedMembers = if (weeklyStatsList.isNotEmpty()) {
            weeklyStatsList.maxOf { it.attendedMembers }
        } else {
            0
        }
        
        val attendanceRate = if (weeklyStatsList.isNotEmpty()) {
            weeklyStatsList.map { it.attendanceRate }.average()
        } else {
            0.0
        }
        
        val averageQtCount = if (weeklyStatsList.isNotEmpty()) {
            weeklyStatsList.map { it.averageQtCount }.average()
        } else {
            0.0
        }
        
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