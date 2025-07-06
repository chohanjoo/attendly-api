package com.attendly.domain.model

/**
 * 마을의 GBS와 리더 정보를 담는 데이터 클래스
 */
data class VillageGbsWithLeaderInfo(
    val gbsId: Long,
    val gbsName: String,
    val leaderId: Long?,
    val leaderName: String?,
    val memberCount: Int
) 