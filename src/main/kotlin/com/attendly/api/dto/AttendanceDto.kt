package com.attendly.api.dto

import com.attendly.domain.entity.MinistryStatus
import com.attendly.domain.entity.WorshipStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

// 출석 요청 DTO
@Schema(description = "출석 일괄 등록 요청")
data class AttendanceBatchRequest(
    @field:NotNull(message = "GBS ID는 필수입니다")
    @Schema(description = "GBS 그룹 ID", example = "1")
    val gbsId: Long,
    
    @field:NotNull(message = "주차 시작일은 필수입니다")
    @Schema(description = "주차 시작일 (일요일 기준)", example = "2025-01-05")
    val weekStart: LocalDate,
    
    @field:Valid
    @Schema(description = "조원별 출석 데이터")
    val attendances: List<AttendanceItemRequest>
)

// 개별 조원 출석 요청 DTO
@Schema(description = "개별 조원 출석 데이터")
data class AttendanceItemRequest(
    @field:NotNull(message = "조원 ID는 필수입니다")
    @Schema(description = "조원 ID", example = "5")
    val memberId: Long,
    
    @field:NotNull(message = "예배 출석 여부는 필수입니다")
    @Schema(description = "예배 출석 상태 (O/X)", example = "O", allowableValues = ["O", "X"])
    val worship: WorshipStatus,
    
    @field:NotNull(message = "QT 횟수는 필수입니다")
    @field:Min(value = 0, message = "QT 횟수는 0-6 사이어야 합니다")
    @field:Max(value = 6, message = "QT 횟수는 0-6 사이어야 합니다")
    @Schema(description = "QT 횟수 (0-6)", example = "5", minimum = "0", maximum = "6")
    val qtCount: Int,
    
    @field:NotNull(message = "대학부 출석 등급은 필수입니다")
    @Schema(description = "대학부 출석 등급 (A/B/C)", example = "A", allowableValues = ["A", "B", "C"])
    val ministry: MinistryStatus
)

// 출석 응답 DTO (목록용)
@Schema(description = "출석 목록 응답")
data class AttendanceResponse(
    @Schema(description = "출석 ID", example = "1")
    val id: Long,
    
    @Schema(description = "조원 ID", example = "5")
    val memberId: Long,
    
    @Schema(description = "조원 이름", example = "홍길동")
    val memberName: String,
    
    @Schema(description = "주차 시작일", example = "2025-01-05")
    val weekStart: LocalDate,
    
    @Schema(description = "예배 출석 상태", example = "O")
    val worship: WorshipStatus,
    
    @Schema(description = "QT 횟수", example = "5")
    val qtCount: Int,
    
    @Schema(description = "대학부 출석 등급", example = "A")
    val ministry: MinistryStatus
)

// 마을 출석 현황 응답 DTO
@Schema(description = "마을 출석 현황 응답")
data class VillageAttendanceResponse(
    @Schema(description = "마을 ID", example = "1")
    val villageId: Long,
    
    @Schema(description = "마을 이름", example = "1마을")
    val villageName: String,
    
    @Schema(description = "주차 시작일", example = "2025-01-05")
    val weekStart: LocalDate,
    
    @Schema(description = "GBS별 출석 현황")
    val gbsAttendances: List<GbsAttendanceSummary>
)

// GBS 출석 요약 DTO
@Schema(description = "GBS 출석 요약")
data class GbsAttendanceSummary(
    @Schema(description = "GBS 그룹 ID", example = "1")
    val gbsId: Long,
    
    @Schema(description = "GBS 그룹 이름", example = "믿음 GBS")
    val gbsName: String,
    
    @Schema(description = "리더 이름", example = "이리더")
    val leaderName: String,
    
    @Schema(description = "총 인원", example = "8")
    val totalMembers: Int,
    
    @Schema(description = "출석 인원", example = "6")
    val attendedMembers: Int,
    
    @Schema(description = "출석률", example = "75.0")
    val attendanceRate: Double,
    
    @Schema(description = "조원별 출석 현황")
    val memberAttendances: List<AttendanceResponse>
)

// 마을장의 출석 수정 요청 DTO
@Schema(description = "마을장의 출석 수정 요청")
data class AttendanceUpdateRequestDto(
    @field:NotNull(message = "GBS ID는 필수입니다")
    @Schema(description = "GBS 그룹 ID", example = "1")
    val gbsId: Long,
    
    @field:NotNull(message = "주차 시작일은 필수입니다")
    @Schema(description = "주차 시작일 (일요일 기준)", example = "2025-01-05")
    val weekStart: LocalDate,
    
    @field:Valid
    @Schema(description = "조원별 출석 데이터")
    val attendances: List<AttendanceMemberItemDto>
)

// 마을장의 개별 조원 출석 데이터 DTO
@Schema(description = "마을장의 개별 조원 출석 데이터")
data class AttendanceMemberItemDto(
    @field:NotNull(message = "조원 ID는 필수입니다")
    @Schema(description = "조원 ID", example = "5")
    val memberId: Long,
    
    @field:NotNull(message = "예배 출석 여부는 필수입니다")
    @Schema(description = "예배 출석 상태 (O/X)", example = "O", allowableValues = ["O", "X"])
    val worship: WorshipStatus,
    
    @field:NotNull(message = "QT 횟수는 필수입니다")
    @field:Min(value = 0, message = "QT 횟수는 0-6 사이어야 합니다")
    @field:Max(value = 6, message = "QT 횟수는 0-6 사이어야 합니다")
    @Schema(description = "QT 횟수 (0-6)", example = "5", minimum = "0", maximum = "6")
    val qtCount: Int,
    
    @field:NotNull(message = "대학부 출석 등급은 필수입니다")
    @Schema(description = "대학부 출석 등급 (A/B/C)", example = "A", allowableValues = ["A", "B", "C"])
    val ministry: MinistryStatus
) 