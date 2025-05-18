package com.attendly.domain.model

import com.attendly.enums.Role

data class UserFilterDto(
    val name: String? = null,
    val departmentId: Long? = null,
    val villageId: Long? = null,
    val roles: List<Role>? = null
)