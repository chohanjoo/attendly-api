package com.attendly.domain.entity

import java.io.Serializable

/**
 * VillageLeader의 복합 키 클래스
 */
class VillageLeaderId(
    var user: Long? = null,
    var village: Long? = null
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VillageLeaderId

        if (user != other.user) return false
        if (village != other.village) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user?.hashCode() ?: 0
        result = 31 * result + (village?.hashCode() ?: 0)
        return result
    }
}