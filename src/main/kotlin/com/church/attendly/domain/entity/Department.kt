package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "department")
class Department(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 50)
    val name: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "department")
    val villages: MutableList<Village> = mutableListOf(),

    @OneToMany(mappedBy = "department")
    val users: MutableList<User> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Department

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Department(id=$id, name='$name')"
    }
} 