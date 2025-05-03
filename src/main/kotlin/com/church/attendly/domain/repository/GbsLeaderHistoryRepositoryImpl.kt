package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.QGbsLeaderHistory
import com.church.attendly.domain.entity.User
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport

class GbsLeaderHistoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsLeaderHistory::class.java), GbsLeaderHistoryRepositoryCustom {

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

    override fun findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId: Long, leaderId: Long): GbsLeaderHistory? {
        return queryFactory
            .selectFrom(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsId),
                gbsLeaderHistory.leader.id.eq(leaderId),
                gbsLeaderHistory.endDate.isNull
            )
            .fetchOne()
    }
    
    override fun findCurrentLeaderByGbsGroupId(gbsGroupId: Long): GbsLeaderHistory? {
        return queryFactory
            .selectFrom(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsGroupId),
                gbsLeaderHistory.endDate.isNull
            )
            .fetchOne()
    }

    override fun findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId: Long, leaderId: Long): GbsLeaderHistory? {
        return queryFactory
            .selectFrom(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsId),
                gbsLeaderHistory.leader.id.eq(leaderId),
                gbsLeaderHistory.endDate.isNull
            )
            .fetchOne()
    }
    
    override fun findByLeaderIdAndEndDateIsNull(leaderId: Long): GbsLeaderHistory? {
        return queryFactory
            .selectFrom(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.leader.id.eq(leaderId),
                gbsLeaderHistory.endDate.isNull
            )
            .fetchOne()
    }
} 