package com.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "attendance")
class Attendance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gbs_id", nullable = false)
    val gbsGroup: GbsGroup,

    @Column(name = "week_start", nullable = false)
    val weekStart: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val worship: WorshipStatus,

    @Column(name = "qt_count", nullable = false)
    val qtCount: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val ministry: MinistryStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: User,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Attendance

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Attendance(id=$id, member=${member.id}, week=$weekStart)"
    }
}

enum class WorshipStatus {
    O, X
}

enum class MinistryStatus {
    A, B, C
} 