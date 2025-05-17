package com.attendly.api.dto

data class GbsGroupListResponse(
    val groups: List<GbsGroupInfo>
)

data class GbsGroupInfo(
    val id: Long,
    val name: String,
    val description: String,
    val color: String,
    val isActive: Boolean
) 