package com.attendly.api.dto

import java.time.LocalDateTime

/**
 * 사용자가 속한 마을 정보 응답 DTO
 */
data class UserVillageResponse(
    val userId: Long,
    val userName: String,
    val villageId: Long,
    val villageName: String,
    val departmentId: Long,
    val departmentName: String,
    val isVillageLeader: Boolean
) 