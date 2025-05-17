package com.attendly.api.dto

/**
 * GBS 리더 후보 목록 응답 DTO
 */
data class LeaderCandidateResponse(
    val candidates: List<LeaderCandidate>
)

/**
 * GBS 리더 후보 정보 DTO
 */
data class LeaderCandidate(
    val id: Long,
    val name: String,
    val email: String?,
    val isLeader: Boolean,
    val previousGbsCount: Int
) 