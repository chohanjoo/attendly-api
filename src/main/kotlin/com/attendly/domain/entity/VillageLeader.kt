package com.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "village_leader")
@IdClass(VillageLeaderId::class)
class VillageLeader(
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: User,

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id")
    val village: Village,

    @Column(name = "start_dt", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_dt")
    val endDate: LocalDate? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VillageLeader

        if (user.id != other.user.id) return false
        if (village.id != other.village.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.id?.hashCode() ?: 0
        result = 31 * result + (village.id?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "VillageLeader(user=${user.id}, village=${village.id})"
    }
} 