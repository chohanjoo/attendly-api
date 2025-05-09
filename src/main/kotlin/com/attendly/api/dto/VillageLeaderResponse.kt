package com.attendly.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "마을장 정보 응답")
data class VillageLeaderResponse(
    @Schema(description = "사용자 ID", example = "1")
    val userId: Long,

    @Schema(description = "사용자 이름", example = "홍길동")
    val userName: String,

    @Schema(description = "마을 ID", example = "1")
    val villageId: Long,

    @Schema(description = "마을 이름", example = "동문마을")
    val villageName: String,

    @Schema(description = "임기 시작일", example = "2023-01-01")
    val startDate: LocalDate,

    @Schema(description = "임기 종료일", example = "2023-12-31")
    val endDate: LocalDate? = null,

    @Schema(description = "생성일시", example = "2023-01-01T12:00:00")
    val createdAt: LocalDateTime,

    @Schema(description = "수정일시", example = "2023-01-01T12:00:00")
    val updatedAt: LocalDateTime
) 