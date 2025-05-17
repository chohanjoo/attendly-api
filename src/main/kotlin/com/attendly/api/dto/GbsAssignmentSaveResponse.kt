package com.attendly.api.dto

/**
 * GBS 배치 정보 저장 응답 DTO
 */
data class GbsAssignmentSaveResponse(
    val villageId: Long,
    val assignmentCount: Int,
    val memberCount: Int,
    val message: String
) 