package com.attendly.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

// 부서별 출석 통계 DTO
@Schema(description = "부서별 출석 통계")
data class DepartmentStatistics(
    @Schema(description = "부서 ID", example = "1")
    val departmentId: Long,
    
    @Schema(description = "부서 이름", example = "대학부")
    val departmentName: String,
    
    @Schema(description = "기간 시작일", example = "2025-01-01")
    val startDate: LocalDate,
    
    @Schema(description = "기간 종료일", example = "2025-01-31")
    val endDate: LocalDate,
    
    @Schema(description = "마을별 통계")
    val villageStats: List<VillageStatistics>,
    
    @Schema(description = "총원", example = "120")
    val totalMembers: Int,
    
    @Schema(description = "출석인원", example = "92")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "76.7")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "3.5")
    val averageQtCount: Double
)

// 마을별 출석 통계 DTO
@Schema(description = "마을별 출석 통계")
data class VillageStatistics(
    @Schema(description = "마을 ID", example = "1")
    val villageId: Long,
    
    @Schema(description = "마을 이름", example = "1마을")
    val villageName: String,
    
    @Schema(description = "GBS별 통계")
    val gbsStats: List<GbsStatistics>,
    
    @Schema(description = "총원", example = "30")
    val totalMembers: Int,
    
    @Schema(description = "출석인원", example = "25")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "83.3")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "4.1")
    val averageQtCount: Double
)

// GBS별 출석 통계 DTO
@Schema(description = "GBS별 출석 통계")
data class GbsStatistics(
    @Schema(description = "GBS 그룹 ID", example = "1")
    val gbsId: Long,
    
    @Schema(description = "GBS 그룹 이름", example = "믿음 GBS")
    val gbsName: String,
    
    @Schema(description = "리더 이름", example = "이리더")
    val leaderName: String,
    
    @Schema(description = "총원", example = "8")
    val totalMembers: Int,
    
    @Schema(description = "출석인원", example = "7")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "87.5")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "4.3")
    val averageQtCount: Double,
    
    @Schema(description = "주별 통계")
    val weeklyStats: List<WeeklyStatistics>
)

// 주별 통계 DTO
@Schema(description = "주별 통계")
data class WeeklyStatistics(
    @Schema(description = "주차 시작일", example = "2025-01-05")
    val weekStart: LocalDate,
    
    @Schema(description = "총원", example = "8")
    val totalMembers: Int,
    
    @Schema(description = "출석인원", example = "6")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "75.0")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "3.8")
    val averageQtCount: Double
) 