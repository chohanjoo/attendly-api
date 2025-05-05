package com.attendly.domain.entity

import com.attendly.api.dto.BatchJobStatus
import com.attendly.api.dto.BatchJobType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "batch_jobs")
class BatchJob(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val jobType: BatchJobType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: BatchJobStatus = BatchJobStatus.QUEUED,

    @Column(name = "parameters", columnDefinition = "TEXT")
    var parameters: String = "{}",

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var startedAt: LocalDateTime? = null,

    @Column
    var finishedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column
    var scheduleTime: LocalDateTime? = null,

    @OneToMany(mappedBy = "batchJob", cascade = [CascadeType.ALL], orphanRemoval = true)
    val logs: MutableList<BatchJobLog> = mutableListOf()
) {
    fun addLog(message: String, level: String = "INFO") {
        logs.add(
            BatchJobLog(
                batchJob = this,
                message = message,
                level = level,
                timestamp = LocalDateTime.now()
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BatchJob

        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "BatchJob(id=$id, jobType=$jobType, status=$status)"
    }
} 