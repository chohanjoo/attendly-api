package com.attendly.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "마을내 모든 GBS 정보 응답")
data class VillageGbsInfoResponse(
    @Schema(description = "마을 ID")
    val villageId: Long,
    
    @Schema(description = "마을 이름")
    val villageName: String,
    
    @Schema(description = "마을 내 GBS 수")
    val gbsCount: Int,
    
    @Schema(description = "마을 내 총 조원 수")
    val totalMemberCount: Int,
    
    @Schema(description = "GBS 리스트")
    val gbsList: List<GbsInfo>
) {
    @Schema(description = "GBS 상세 정보")
    data class GbsInfo(
        @Schema(description = "GBS ID")
        val gbsId: Long,
        
        @Schema(description = "GBS 이름")
        val gbsName: String,
        
        @Schema(description = "리더 ID")
        val leaderId: Long?,
        
        @Schema(description = "리더 이름")
        val leaderName: String,
        
        @Schema(description = "조원 수")
        val memberCount: Int,
        
        @Schema(description = "조원 리스트")
        val members: List<GbsMemberResponse>
    )
} 