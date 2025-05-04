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

/**
 * 리더의 GBS 히스토리 리스트 응답 DTO
 */
data class LeaderGbsHistoryListResponse(
    val leaderId: Long,
    val leaderName: String,
    val historyCount: Int,
    val histories: List<LeaderGbsHistoryResponse>
)

/**
 * 리더의 개별 GBS 히스토리 응답 DTO
 */
data class LeaderGbsHistoryResponse(
    val historyId: Long,
    val gbsId: Long,
    val gbsName: String,
    val villageId: Long,
    val villageName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val members: List<GbsMemberResponse>
) 