package com.attendly.api.dto

import java.time.LocalDate

data class VillageMemberResponse(
    val members: List<MemberInfo>,
    val totalCount: Int,
    val villageId: Long,
    val villageName: String
)

data class MemberInfo(
    val id: Long,
    val name: String,
    val birthDate: LocalDate?,
    val email: String?,
    val phoneNumber: String?,
    val role: String,
    val joinDate: LocalDate?
) 