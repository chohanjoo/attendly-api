package com.attendly.domain.repository

import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.QGbsLeaderHistory
import com.attendly.domain.entity.User
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

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

    override fun findLeaderByGbsIdAndDate(gbsId: Long, date: LocalDate): User? {
        return queryFactory
            .select(gbsLeaderHistory.leader)
            .from(gbsLeaderHistory)
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsId),
                gbsLeaderHistory.startDate.loe(date),
                gbsLeaderHistory.endDate.isNull.or(gbsLeaderHistory.endDate.goe(date))
            )
            .fetchOne()
    }
    
    override fun findCurrentLeaderHistoryByGbsId(gbsId: Long, date: LocalDate): GbsLeaderHistory? {
        return queryFactory
            .selectFrom(gbsLeaderHistory)
            .join(gbsLeaderHistory.leader).fetchJoin()
            .where(
                gbsLeaderHistory.gbsGroup.id.eq(gbsId),
                gbsLeaderHistory.startDate.loe(date),
                gbsLeaderHistory.endDate.isNull.or(gbsLeaderHistory.endDate.goe(date))
            )
            .fetchOne()
    }
} 