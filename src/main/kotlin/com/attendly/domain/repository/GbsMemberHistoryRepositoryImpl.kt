package com.attendly.domain.repository

import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.entity.QGbsMemberHistory
import com.attendly.domain.model.GbsMemberHistorySearchCondition
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
    
    override fun findCurrentMemberHistoryByMemberId(memberId: Long): GbsMemberHistory? {
        return queryFactory
            .selectFrom(gbsMemberHistory)
            .where(
                gbsMemberHistory.member.id.eq(memberId),
                gbsMemberHistory.endDate.isNull
            )
            .fetchOne()
    }
    
    override fun findCurrentMembersByGbsId(gbsId: Long, date: LocalDate): List<GbsMemberHistory> {
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

    override fun findActiveMembersByVillageGbsIds(gbsIds: List<Long>, date: LocalDate): List<GbsMemberHistory> {
        if (gbsIds.isEmpty()) {
            return emptyList()
        }
        
        return queryFactory
            .selectFrom(gbsMemberHistory)
            .join(gbsMemberHistory.member).fetchJoin()
            .join(gbsMemberHistory.gbsGroup).fetchJoin()
            .where(
                gbsMemberHistory.gbsGroup.id.`in`(gbsIds),
                gbsMemberHistory.startDate.loe(date),
                gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(date))
            )
            .orderBy(gbsMemberHistory.gbsGroup.id.asc(), gbsMemberHistory.member.name.asc())
            .fetch()
    }
} 