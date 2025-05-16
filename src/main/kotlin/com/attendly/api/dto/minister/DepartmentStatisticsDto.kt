package com.attendly.api.dto.minister

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "부서 통계 요약 응답")
data class DepartmentStatisticsResponse(
    @Schema(description = "부서 ID", example = "1")
    val departmentId: Long,
    
    @Schema(description = "부서명", example = "대학부")
    val departmentName: String,
    
    @Schema(description = "총 멤버 수", example = "120")
    val totalMembers: Int,
    
    @Schema(description = "출석 멤버 수", example = "98")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "81.7")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "4.2")
    val averageQtCount: Double,
    
    @Schema(description = "마을별 통계")
    val villages: List<VillageStatisticsItem>,
    
    @Schema(description = "주간 통계")
    val weeklyStats: List<WeeklyStatisticsItem>
)

@Schema(description = "마을 통계 항목")
data class VillageStatisticsItem(
    @Schema(description = "마을 ID", example = "1")
    val villageId: Long,
    
    @Schema(description = "마을명", example = "동문 마을")
    val villageName: String,
    
    @Schema(description = "총 멤버 수", example = "24")
    val totalMembers: Int,
    
    @Schema(description = "출석 멤버 수", example = "22")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "91.7")
    val attendanceRate: Double,
    
    @Schema(description = "평균 QT 횟수", example = "4.8")
    val averageQtCount: Double
)

@Schema(description = "주간 통계 항목")
data class WeeklyStatisticsItem(
    @Schema(description = "주 시작일", example = "2023-09-03")
    val weekStart: LocalDate,
    
    @Schema(description = "총 멤버 수", example = "120")
    val totalMembers: Int,
    
    @Schema(description = "출석 멤버 수", example = "95")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "79.2")
    val attendanceRate: Double
) 