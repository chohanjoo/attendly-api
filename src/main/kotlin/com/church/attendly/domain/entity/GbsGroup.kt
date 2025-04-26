package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "gbs_group")
class GbsGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "village_id", nullable = false)
    val village: Village,

    @Column(name = "term_start_dt", nullable = false)
    val termStartDate: LocalDate,

    @Column(name = "term_end_dt", nullable = false)
    val termEndDate: LocalDate,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "gbsGroup")
    val leaderHistories: MutableList<GbsLeaderHistory> = mutableListOf(),

    @OneToMany(mappedBy = "gbsGroup")
    val memberHistories: MutableList<GbsMemberHistory> = mutableListOf(),

    @OneToMany(mappedBy = "gbsGroup")
    val attendances: MutableList<Attendance> = mutableListOf(),

    @OneToMany(mappedBy = "gbsGroup")
    val leaderDelegations: MutableList<LeaderDelegation> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GbsGroup

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "GbsGroup(id=$id, name='$name')"
    }
} 