package com.church.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_job_logs")
class BatchJobLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_job_id", nullable = false)
    val batchJob: BatchJob,

    @Column(nullable = false, columnDefinition = "TEXT")
    val message: String,

    @Column(length = 10, nullable = false)
    val level: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BatchJobLog

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "BatchJobLog(id=$id, level='$level', message='${message.take(50)}${if (message.length > 50) "..." else ""}')"
    }
} 