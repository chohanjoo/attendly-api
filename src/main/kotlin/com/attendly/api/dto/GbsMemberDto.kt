package com.attendly.api.dto

import com.attendly.domain.entity.User
import java.time.LocalDate

data class GbsMemberSearchDto(
    val gbsId: Long,
    val targetDate: LocalDate,
    val currentUser: User
)

data class LeaderGbsHistoryRequestDto(
    val leaderId: Long,
    val currentUser: User
)

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

data class LeaderCandidateResponse(
    val villageId: Long,
    val villageName: String,
    val candidates: List<LeaderCandidate>
) 