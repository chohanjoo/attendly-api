package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "system_settings")
class SystemSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "setting_key", nullable = false, unique = true, length = 50)
    val key: String,

    @Column(name = "setting_value", nullable = false, length = 1000)
    var value: String,

    @Column(length = 200)
    var description: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SystemSetting

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "SystemSetting(id=$id, key='$key', value='$value')"
    }
} 