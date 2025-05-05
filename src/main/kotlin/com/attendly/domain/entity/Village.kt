package com.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "village")
class Village(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    val department: Department,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "village")
    val gbsGroups: MutableList<GbsGroup> = mutableListOf(),

    @OneToOne(mappedBy = "village")
    val villageLeader: VillageLeader? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Village

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Village(id=$id, name='$name')"
    }
} 