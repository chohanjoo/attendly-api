package com.attendly.api.dto.minister

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "마을 상세 통계 응답")
data class VillageDetailStatisticsResponse(
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
    val averageQtCount: Double,
    
    @Schema(description = "멤버별 통계")
    val members: List<MemberStatisticsItem>,
    
    @Schema(description = "주간 통계")
    val weeklyStats: List<VillageWeeklyStatisticsItem>
)

@Schema(description = "멤버 통계 항목")
data class MemberStatisticsItem(
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    
    @Schema(description = "사용자 이름", example = "홍길동")
    val userName: String,
    
    @Schema(description = "출석 횟수", example = "4")
    val attendanceCount: Int,
    
    @Schema(description = "출석률", example = "100.0")
    val attendanceRate: Double,
    
    @Schema(description = "QT 횟수", example = "5")
    val qtCount: Int
)

@Schema(description = "마을 주간 통계 항목")
data class VillageWeeklyStatisticsItem(
    @Schema(description = "주 시작일", example = "2023-09-03")
    val weekStart: String,
    
    @Schema(description = "총 멤버 수", example = "24")
    val totalMembers: Int,
    
    @Schema(description = "출석 멤버 수", example = "22")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "91.7")
    val attendanceRate: Double
) 