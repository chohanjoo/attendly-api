package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.LeaderDelegation
import com.church.attendly.domain.entity.QLeaderDelegation
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

class LeaderDelegationRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(LeaderDelegation::class.java), LeaderDelegationRepositoryCustom {

    private val leaderDelegation = QLeaderDelegation.leaderDelegation

    override fun findActiveDelegationsByDelegateeAndGbs(
        delegateeId: Long,
        gbsId: Long,
        date: LocalDate
    ): List<LeaderDelegation> {
        return queryFactory
            .selectFrom(leaderDelegation)
            .where(
                leaderDelegation.delegatee.id.eq(delegateeId),
                leaderDelegation.gbsGroup.id.eq(gbsId),
                leaderDelegation.startDate.loe(date),
                leaderDelegation.endDate.isNull.or(leaderDelegation.endDate.goe(date))
            )
            .fetch()
    }
    
    override fun findActiveByGbsGroupIdAndDate(gbsGroupId: Long, date: LocalDate): LeaderDelegation? {
        return queryFactory
            .selectFrom(leaderDelegation)
            .where(
                leaderDelegation.gbsGroup.id.eq(gbsGroupId),
                leaderDelegation.startDate.loe(date),
                leaderDelegation.endDate.isNull.or(leaderDelegation.endDate.goe(date))
            )
            .fetchOne()
    }
    
    override fun findActiveByDelegateIdAndDate(userId: Long, date: LocalDate): List<LeaderDelegation> {
        return queryFactory
            .selectFrom(leaderDelegation)
            .where(
                leaderDelegation.delegatee.id.eq(userId),
                leaderDelegation.startDate.loe(date),
                leaderDelegation.endDate.isNull.or(leaderDelegation.endDate.goe(date))
            )
            .fetch()
    }
    
    override fun findActiveByGbsIdAndDelegateeId(gbsId: Long, delegateeId: Long, date: LocalDate): LeaderDelegation? {
        return queryFactory
            .selectFrom(leaderDelegation)
            .where(
                leaderDelegation.gbsGroup.id.eq(gbsId),
                leaderDelegation.delegatee.id.eq(delegateeId),
                leaderDelegation.startDate.loe(date),
                leaderDelegation.endDate.isNull.or(leaderDelegation.endDate.goe(date))
            )
            .fetchOne()
    }
} 