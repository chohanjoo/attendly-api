package com.attendly.domain.repository

import com.attendly.api.dto.LeaderGbsHistoryMemberDto
import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.QGbsLeaderHistory
import com.attendly.domain.entity.QGbsMemberHistory
import com.attendly.domain.entity.QUser
import com.attendly.domain.entity.QVillage
import com.attendly.domain.entity.User
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

class GbsLeaderHistoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsLeaderHistory::class.java), GbsLeaderHistoryRepositoryCustom {

    private val gbsLeaderHistory = QGbsLeaderHistory.gbsLeaderHistory
    private val gbsGroup = QGbsGroup.gbsGroup
    private val village = QVillage.village
    private val gbsMemberHistory = QGbsMemberHistory.gbsMemberHistory
    private val member = QUser.user

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
    
    override fun findLeaderGbsHistoriesWithMembers(leaderId: Long): List<LeaderGbsHistoryMemberDto> {
        val currentDate = LocalDate.now()
        
        return queryFactory
            .select(
                Projections.constructor(
                    LeaderGbsHistoryMemberDto::class.java,
                    gbsLeaderHistory.id,
                    gbsGroup.id,
                    gbsGroup.name,
                    village.id,
                    village.name,
                    gbsLeaderHistory.startDate,
                    gbsLeaderHistory.endDate,
                    gbsLeaderHistory.endDate.isNull,
                    member.id,
                    member.name,
                    member.email,
                    member.birthDate,
                    member.phoneNumber,
                    gbsMemberHistory.startDate
                )
            )
            .from(gbsLeaderHistory)
            .join(gbsLeaderHistory.gbsGroup, gbsGroup)
            .join(gbsGroup.village, village)
            .leftJoin(gbsMemberHistory)
            .on(
                gbsMemberHistory.gbsGroup.eq(gbsGroup),
                // 활성 히스토리(endDate가 null)인 경우 현재 날짜 기준으로 멤버 조회
                gbsLeaderHistory.endDate.isNull.and(
                    gbsMemberHistory.startDate.loe(currentDate)
                        .and(gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(currentDate)))
                )
                // 비활성 히스토리인 경우 종료 날짜 기준으로 멤버 조회
                .or(
                    gbsLeaderHistory.endDate.isNotNull.and(
                        gbsMemberHistory.startDate.loe(gbsLeaderHistory.endDate)
                            .and(gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(gbsLeaderHistory.endDate)))
                    )
                )
            )
            .leftJoin(gbsMemberHistory.member, member)
            .where(gbsLeaderHistory.leader.id.eq(leaderId))
            .orderBy(gbsLeaderHistory.startDate.desc(), member.name.asc())
            .fetch()
    }
} 