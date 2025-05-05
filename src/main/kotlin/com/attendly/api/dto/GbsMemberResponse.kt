package com.attendly.api.dto

import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.entity.User
import java.time.LocalDate

data class GbsMemberResponse(
    val id: Long,
    val name: String,
    val email: String?,
    val birthDate: LocalDate?,
    val joinDate: LocalDate,
    val phoneNumber: String? = null
) {
    companion object {
        fun from(memberHistory: GbsMemberHistory): GbsMemberResponse {
            val member = memberHistory.member
            return GbsMemberResponse(
                id = member.id ?: 0,
                name = member.name,
                email = member.email,
                birthDate = member.birthDate,
                joinDate = memberHistory.startDate
            )
        }
    }
}

data class GbsMembersListResponse(
    val gbsId: Long,
    val gbsName: String,
    val memberCount: Int,
    val members: List<GbsMemberResponse>
) 