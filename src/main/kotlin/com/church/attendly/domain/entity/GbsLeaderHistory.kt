package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "gbs_leader_history")
class GbsLeaderHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gbs_id", nullable = false)
    val gbsGroup: GbsGroup,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    val leader: User,

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

        other as GbsLeaderHistory

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "GbsLeaderHistory(id=$id, gbsGroup=${gbsGroup.id}, leader=${leader.id})"
    }
} 