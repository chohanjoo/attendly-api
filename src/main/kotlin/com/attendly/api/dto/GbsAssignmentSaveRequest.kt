package com.attendly.api.dto

import java.time.LocalDate

/**
 * GBS 배치 정보 저장 요청 DTO
 */
data class GbsAssignmentSaveRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val assignments: List<GbsAssignment>
)

/**
 * GBS 배치 정보 DTO
 */
data class GbsAssignment(
    val gbsId: Long,
    val leaderId: Long,
    val memberIds: List<Long>
) 