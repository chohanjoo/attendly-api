package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "leader_delegation")
class LeaderDelegation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegator_id", nullable = false)
    val delegator: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegatee_id", nullable = false)
    val delegatee: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gbs_id", nullable = false)
    val gbsGroup: GbsGroup,

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

        other as LeaderDelegation

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "LeaderDelegation(id=$id, delegator=${delegator.id}, delegatee=${delegatee.id}, gbsGroup=${gbsGroup.id})"
    }
} 