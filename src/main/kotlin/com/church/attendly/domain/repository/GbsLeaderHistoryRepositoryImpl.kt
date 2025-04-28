package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.QGbsLeaderHistory
import com.church.attendly.domain.entity.User
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

class GbsLeaderHistoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(User::class.java), GbsLeaderHistoryRepositoryCustom {

    private val gbsLeaderHistory = QGbsLeaderHistory.gbsLeaderHistory

    override fun findCurrentLeaderByGbsId(gbsId: Long): User? {
        return queryFactory
            .select(gbsLeaderHistory.leader)
            .from(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsId),
                gbsLeaderHistory.endDate.isNull
            )
            .fetchOne()
    }
} 