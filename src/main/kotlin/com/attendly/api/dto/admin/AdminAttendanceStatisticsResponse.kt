package com.attendly.api.dto.admin

/**
 * 관리자용 출석 통계 정보 응답 DTO
 */
data class AdminAttendanceStatisticsResponse(
    val attendanceRate: Double,                 // 총 출석률
    val attendanceRateDifference: Double,       // 전월 대비 출석률 차이
    val totalAttendanceCount: Int,              // 총 출석 인원수 (이번달 기준)
    val absentRate: Double,                     // 결석률
    val absentRateDifference: Double,           // 전월 대비 결석률 차이
    val lateRate: Double,                       // 지각률
    val lateRateDifference: Double              // 전월 대비 지각률 차이
) 