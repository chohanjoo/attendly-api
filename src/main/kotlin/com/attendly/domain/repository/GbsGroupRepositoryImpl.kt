package com.attendly.domain.repository

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.QGbsLeaderHistory
import com.attendly.domain.entity.QUser
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsWithLeader
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class GbsGroupRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsGroup::class.java), GbsGroupRepositoryCustom {

    private val gbsGroup = QGbsGroup.gbsGroup
    private val gbsLeaderHistory = QGbsLeaderHistory.gbsLeaderHistory
    private val user = QUser.user

    override fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup> {
        return queryFactory
            .selectFrom(gbsGroup)
            .join(gbsGroup.village).fetchJoin()
            .where(
                gbsGroup.village.id.eq(villageId),
                gbsGroup.termStartDate.loe(date),
                gbsGroup.termEndDate.goe(date)
            )
            .fetch()
    }

    override fun findByVillageAndTermDate(village: Village, startDate: LocalDate, endDate: LocalDate): List<GbsGroup> {
        return queryFactory
            .selectFrom(gbsGroup)
            .join(gbsGroup.village).fetchJoin()
            .where(
                gbsGroup.village.eq(village),
                gbsGroup.termStartDate.loe(startDate),
                gbsGroup.termEndDate.goe(endDate)
            )
            .fetch()
    }
    
    override fun findWithCurrentLeader(gbsId: Long): GbsWithLeader? {
        val result = queryFactory
            .select(gbsGroup, user)
            .from(gbsGroup)
            .leftJoin(gbsLeaderHistory)
            .on(
                gbsLeaderHistory.gbsGroup.eq(gbsGroup),
                gbsLeaderHistory.endDate.isNull
            )
            .leftJoin(user)
            .on(gbsLeaderHistory.leader.eq(user))
            .where(gbsGroup.id.eq(gbsId))
            .fetchOne() ?: return null
            
        val group = result.get(gbsGroup)!!
        val leader = result.get(user)
        return GbsWithLeader(
            gbsGroup = group,
            leader = leader
        )
    }
} 