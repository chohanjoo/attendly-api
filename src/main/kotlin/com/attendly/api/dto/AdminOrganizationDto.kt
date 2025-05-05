package com.attendly.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime

data class DepartmentCreateRequest(
    @field:NotBlank(message = "부서명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "부서명은 2자 이상 50자 이하로 입력해주세요")
    val name: String
)

data class DepartmentUpdateRequest(
    @field:NotBlank(message = "부서명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "부서명은 2자 이상 50자 이하로 입력해주세요")
    val name: String
)

data class DepartmentResponse(
    val id: Long,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class VillageCreateRequest(
    @field:NotBlank(message = "마을명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "마을명은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    @field:NotNull(message = "부서 ID는 필수입니다")
    val departmentId: Long,

    @field:NotNull(message = "마을장 ID는 필수입니다")
    val villageLeaderId: Long? = null
)

data class VillageUpdateRequest(
    @field:NotBlank(message = "마을명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "마을명은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    val departmentId: Long? = null,
    val villageLeaderId: Long? = null
)

data class VillageResponse(
    val id: Long,
    val name: String,
    val departmentId: Long,
    val departmentName: String,
    val villageLeaderId: Long?,
    val villageLeaderName: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class GbsGroupCreateRequest(
    @field:NotBlank(message = "GBS 그룹명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "GBS 그룹명은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    @field:NotNull(message = "마을 ID는 필수입니다")
    val villageId: Long,

    @field:NotNull(message = "학기 시작일은 필수입니다")
    val termStartDate: LocalDate,

    @field:NotNull(message = "학기 종료일은 필수입니다")
    val termEndDate: LocalDate,

    val leaderId: Long? = null
)

data class GbsGroupUpdateRequest(
    @field:NotBlank(message = "GBS 그룹명은 필수입니다")
    @field:Size(min = 2, max = 50, message = "GBS 그룹명은 2자 이상 50자 이하로 입력해주세요")
    val name: String,

    val villageId: Long? = null,
    val termStartDate: LocalDate? = null,
    val termEndDate: LocalDate? = null
)

data class GbsGroupResponse(
    val id: Long,
    val name: String,
    val villageId: Long,
    val villageName: String,
    val termStartDate: LocalDate,
    val termEndDate: LocalDate,
    val leaderName: String?,
    val leaderId: Long?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class GbsLeaderAssignRequest(
    @field:NotNull(message = "리더 ID는 필수입니다")
    val leaderId: Long,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null
)

data class GbsMemberAssignRequest(
    @field:NotNull(message = "조원 ID는 필수입니다")
    val memberId: Long,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null
)

data class GbsReorganizationRequest(
    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,
    
    @field:NotNull(message = "종료일은 필수입니다")
    val endDate: LocalDate,
    
    @field:NotNull(message = "부서 ID는 필수입니다")
    val departmentId: Long
)

data class GbsReorganizationResponse(
    val departmentId: Long,
    val departmentName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val affectedGbsCount: Int,
    val affectedMemberCount: Int,
    val affectedLeaderCount: Int,
    val completedAt: LocalDateTime
) 