package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import com.church.attendly.domain.entity.QGbsMemberHistory
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

class GbsMemberHistoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsMemberHistory::class.java), GbsMemberHistoryRepositoryCustom {

    private val gbsMemberHistory = QGbsMemberHistory.gbsMemberHistory

    override fun countActiveMembers(gbsId: Long, date: LocalDate): Long {
        return queryFactory
            .select(gbsMemberHistory.count())
            .from(gbsMemberHistory)
            .where(
                gbsMemberHistory.gbsGroup.id.eq(gbsId),
                gbsMemberHistory.startDate.loe(date),
                gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(date))
            )
            .fetchOne() ?: 0L
    }

    override fun findActiveMembers(gbsId: Long, date: LocalDate): List<GbsMemberHistory> {
        return queryFactory
            .selectFrom(gbsMemberHistory)
            .join(gbsMemberHistory.member).fetchJoin()
            .where(
                gbsMemberHistory.gbsGroup.id.eq(gbsId),
                gbsMemberHistory.startDate.loe(date),
                gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(date))
            )
            .fetch()
    }
} 