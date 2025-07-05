package com.attendly.domain.repository

import com.attendly.api.dto.AdminGbsGroupQueryDto
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.QGbsGroup.gbsGroup
import com.attendly.domain.entity.QGbsLeaderHistory.gbsLeaderHistory
import com.attendly.domain.entity.QGbsMemberHistory.gbsMemberHistory
import com.attendly.domain.entity.QUser.user
import com.attendly.domain.entity.QVillage.village
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsWithLeader
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class GbsGroupRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsGroup::class.java), GbsGroupRepositoryCustom {

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
        val result = queryFactory.select(gbsGroup, user)
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
    
    override fun findAllGbsGroupsWithDetails(pageable: Pageable): Page<GbsGroup> {
        val query = queryFactory
            .selectFrom(gbsGroup)
            .join(gbsGroup.village, village).fetchJoin()
            .join(village.department).fetchJoin()
            .orderBy(gbsGroup.id.desc())
        
        val totalCount = queryFactory
            .select(gbsGroup.count())
            .from(gbsGroup)
            .fetchOne() ?: 0L
        
        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        
        return PageImpl(results, pageable, totalCount)
    }

    override fun findAllGbsGroupsWithCompleteDetails(pageable: Pageable): Page<AdminGbsGroupQueryDto> {
        val currentDate = LocalDate.now()
        
        // 멤버 카운트 서브쿼리
        val memberCountSubQuery = queryFactory
            .select(gbsMemberHistory.count())
            .from(gbsMemberHistory)
            .where(
                gbsMemberHistory.gbsGroup.id.eq(gbsGroup.id),
                gbsMemberHistory.startDate.loe(currentDate),
                gbsMemberHistory.endDate.isNull
                    .or(gbsMemberHistory.endDate.goe(currentDate))
            )
        
        val query = queryFactory
            .select(
                Projections.constructor(
                    AdminGbsGroupQueryDto::class.java,
                    gbsGroup.id,
                    gbsGroup.name,
                    village.id,
                    village.name,
                    gbsGroup.termStartDate,
                    gbsGroup.termEndDate,
                    user.id,
                    user.name,
                    gbsGroup.createdAt,
                    gbsGroup.updatedAt,
                    memberCountSubQuery
                )
            )
            .from(gbsGroup)
            .join(gbsGroup.village, village)
            .leftJoin(gbsLeaderHistory)
            .on(
                gbsLeaderHistory.gbsGroup.eq(gbsGroup),
                gbsLeaderHistory.startDate.loe(currentDate),
                gbsLeaderHistory.endDate.isNull
                    .or(gbsLeaderHistory.endDate.goe(currentDate))
            )
            .leftJoin(gbsLeaderHistory.leader, user)
            .orderBy(gbsGroup.id.desc())
        
        val totalCount = queryFactory
            .select(gbsGroup.count())
            .from(gbsGroup)
            .fetchOne() ?: 0L
        
        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
        
        return PageImpl(results, pageable, totalCount)
    }
} 