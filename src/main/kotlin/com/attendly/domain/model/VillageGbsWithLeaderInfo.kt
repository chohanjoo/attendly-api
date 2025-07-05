package com.attendly.domain.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 마을의 GBS와 리더 정보를 담는 데이터 클래스
 */
data class VillageGbsWithLeaderInfo(
    @JsonProperty("gbsId")
    val gbsId: Long,
    
    @JsonProperty("gbsName")
    val gbsName: String,
    
    @JsonProperty("leaderId")
    val leaderId: Long?,
    
    @JsonProperty("leaderName")
    val leaderName: String?,
    
    @JsonProperty("memberCount")
    val memberCount: Int
) 