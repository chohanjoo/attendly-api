package com.attendly.api.dto

import java.time.LocalDate

data class MemberInfo(
    val id: Long,
    val name: String,
    val birthDate: LocalDate?,
    val email: String?,
    val phoneNumber: String?,
    val role: String,
    val joinDate: LocalDate?
) 