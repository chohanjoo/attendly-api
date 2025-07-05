package com.attendly.api.dto

import java.time.LocalDate

/**
 * 리더 GBS 히스토리 조회를 위한 요청 DTO
 */
data class LeaderGbsHistoryRequestDto(
    val leaderId: Long,
    val currentUser: com.attendly.domain.entity.User
)

/**
 * 리더 GBS 히스토리와 멤버 정보를 함께 조회하기 위한 DTO
 */
data class LeaderGbsHistoryMemberDto(
    val historyId: Long,
    val gbsId: Long,
    val gbsName: String,
    val villageId: Long,
    val villageName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val isActive: Boolean,
    val memberId: Long?,
    val memberName: String?,
    val memberEmail: String?,
    val memberBirthDate: LocalDate?,
    val memberPhoneNumber: String?,
    val memberJoinDate: LocalDate?
) 