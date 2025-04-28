package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import com.church.attendly.domain.entity.QGbsMemberHistory
import com.church.attendly.domain.model.GbsMemberHistorySearchCondition
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
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

    override fun findActiveMembers(condition: GbsMemberHistorySearchCondition): List<GbsMemberHistory> {
        return queryFactory
            .selectFrom(gbsMemberHistory)
            .join(gbsMemberHistory.member).fetchJoin()
            .where(
                gbsMemberHistory.gbsGroup.id.eq(condition.gbsId),
                gbsMemberHistory.startDate.loe(condition.endDate),
                gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(condition.startDate))
            )
            .fetch()
    }
} 