package com.church.attendly.domain.repository

import com.church.attendly.api.dto.BatchJobStatus
import com.church.attendly.api.dto.BatchJobType
import com.church.attendly.domain.entity.BatchJob
import com.church.attendly.domain.entity.QBatchJob
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class BatchJobRepositoryImpl(
    private val entityManager: EntityManager,
    private val queryFactory: JPAQueryFactory
) : BatchJobRepositoryCustom {

    override fun findJobsByFilters(
        jobType: BatchJobType?,
        status: BatchJobStatus?,
        startDateFrom: LocalDateTime?,
        startDateTo: LocalDateTime?,
        endDateFrom: LocalDateTime?,
        endDateTo: LocalDateTime?,
        pageable: Pageable
    ): Page<BatchJob> {
        val batchJob = QBatchJob.batchJob
        val predicate = BooleanBuilder()

        jobType?.let { predicate.and(batchJob.jobType.eq(it)) }
        status?.let { predicate.and(batchJob.status.eq(it)) }
        
        startDateFrom?.let { predicate.and(batchJob.startedAt.goe(it)) }
        startDateTo?.let { predicate.and(batchJob.startedAt.loe(it)) }
        
        endDateFrom?.let { predicate.and(batchJob.finishedAt.goe(it)) }
        endDateTo?.let { predicate.and(batchJob.finishedAt.loe(it)) }

        val query = queryFactory
            .selectFrom(batchJob)
            .where(predicate)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())

        // 정렬 적용
        pageable.sort.forEach { order ->
            if (order.property == "createdAt") {
                if (order.isAscending) {
                    query.orderBy(batchJob.createdAt.asc())
                } else {
                    query.orderBy(batchJob.createdAt.desc())
                }
            } else if (order.property == "startedAt") {
                if (order.isAscending) {
                    query.orderBy(batchJob.startedAt.asc())
                } else {
                    query.orderBy(batchJob.startedAt.desc())
                }
            } else if (order.property == "finishedAt") {
                if (order.isAscending) {
                    query.orderBy(batchJob.finishedAt.asc())
                } else {
                    query.orderBy(batchJob.finishedAt.desc())
                }
            }
        }

        val results = query.fetch()
        
        // 전체 카운트 쿼리
        val countQuery = queryFactory
            .select(batchJob.count())
            .from(batchJob)
            .where(predicate)
        
        val total = countQuery.fetchOne() ?: 0L

        return PageImpl(results, pageable, total)
    }
} 