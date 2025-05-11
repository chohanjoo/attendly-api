package com.attendly.domain.entity

import com.attendly.enums.Role
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(name = "birth_date")
    val birthDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role,

    @Column(unique = true, length = 100)
    val email: String? = null,

    @Column(name = "phone_number", length = 20)
    val phoneNumber: String? = null,

    @Column(length = 100)
    val password: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    val department: Department,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "member")
    val attendances: MutableList<Attendance> = mutableListOf(),

    @OneToMany(mappedBy = "leader")
    val gbsLeaderHistories: MutableList<GbsLeaderHistory> = mutableListOf(),

    @OneToMany(mappedBy = "member")
    val gbsMemberHistories: MutableList<GbsMemberHistory> = mutableListOf(),

    @OneToMany(mappedBy = "delegator")
    val delegationsGiven: MutableList<LeaderDelegation> = mutableListOf(),

    @OneToMany(mappedBy = "delegatee")
    val delegationsReceived: MutableList<LeaderDelegation> = mutableListOf(),

    @OneToOne(mappedBy = "user")
    val villageLeader: VillageLeader? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "User(id=$id, name='$name', role=$role)"
    }
}

