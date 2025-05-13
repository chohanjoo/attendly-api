package com.attendly.api.dto.admin

import com.attendly.enums.AttendanceStatus
import com.attendly.enums.MinistryStatus
import com.attendly.enums.WorshipStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "관리자용 출석 조회 필터")
data class AdminAttendanceSearchRequest(
    @Schema(description = "페이지 번호", example = "0")
    val page: Int = 0,
    
    @Schema(description = "페이지 크기", example = "20")
    val size: Int = 20,
    
    @Schema(description = "검색어 (이름)", example = "홍길동")
    val search: String? = null,
    
    @Schema(description = "시작 날짜", example = "2025-01-01")
    val startDate: LocalDate? = null,
    
    @Schema(description = "종료 날짜", example = "2025-01-31")
    val endDate: LocalDate? = null,
    
    @Schema(description = "출석 상태", example = "PRESENT", allowableValues = ["PRESENT", "ABSENT", "LATE", "EXCUSED"])
    val status: AttendanceStatus? = null
)

@Schema(description = "관리자용 출석 기록 응답")
data class AdminAttendanceResponse(
    @Schema(description = "출석 ID", example = "1")
    val id: Long,
    
    @Schema(description = "사용자 ID", example = "5")
    val userId: Long,
    
    @Schema(description = "사용자 이름", example = "홍길동")
    val userName: String,
    
    @Schema(description = "날짜", example = "2025-01-05")
    val date: LocalDate,
    
    @Schema(description = "출석 상태", example = "PRESENT")
    val status: AttendanceStatus,
    
    @Schema(description = "이벤트 유형", example = "주일예배")
    val eventType: String,
    
    @Schema(description = "비고", example = "병가")
    val note: String? = null
) {
    companion object {
        fun fromEntity(
            id: Long,
            userId: Long,
            userName: String,
            date: LocalDate,
            worship: WorshipStatus,
            qtCount: Int,
            ministry: MinistryStatus
        ): AdminAttendanceResponse {
            // WorshipStatus와 MinistryStatus를 AttendanceStatus로 변환하는 로직
            val status = when {
                worship == WorshipStatus.O && ministry == MinistryStatus.A -> AttendanceStatus.PRESENT
                worship == WorshipStatus.O && ministry == MinistryStatus.B -> AttendanceStatus.LATE
                worship == WorshipStatus.X && qtCount > 0 -> AttendanceStatus.EXCUSED
                else -> AttendanceStatus.ABSENT
            }
            
            // 이벤트 유형과 비고 설정
            val eventType = "주일예배"
            val note = when (status) {
                AttendanceStatus.EXCUSED -> "QT ${qtCount}회 수행"
                AttendanceStatus.LATE -> "지각"
                else -> null
            }
            
            return AdminAttendanceResponse(
                id = id,
                userId = userId,
                userName = userName,
                date = date,
                status = status,
                eventType = eventType,
                note = note
            )
        }
    }
} 