package com.church.attendly.api.dto

import java.time.LocalDate

/**
 * 리더가 속한 GBS 정보 응답 DTO
 */
data class LeaderGbsResponse(
    val gbsId: Long,
    val gbsName: String,
    val villageId: Long,
    val villageName: String,
    val leaderId: Long,
    val leaderName: String,
    val startDate: LocalDate
) 