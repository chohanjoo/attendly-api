package com.attendly.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "마을장 할당 요청")
data class VillageLeaderAssignRequest(
    @field:NotNull(message = "사용자 ID는 필수입니다.")
    @Schema(description = "마을장으로 지정할 사용자 ID", example = "1")
    val userId: Long,

    @field:NotNull(message = "마을 ID는 필수입니다.")
    @Schema(description = "마을장이 관리할 마을 ID", example = "1")
    val villageId: Long,

    @field:NotNull(message = "시작일은 필수입니다.")
    @Schema(description = "마을장 임기 시작일", example = "2023-01-01")
    val startDate: LocalDate,

    @Schema(description = "마을장 임기 종료일 (옵션)", example = "2023-12-31")
    val endDate: LocalDate? = null
) 