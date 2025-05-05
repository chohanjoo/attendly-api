package com.attendly.domain.repository

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.Village
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class GbsGroupRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(GbsGroup::class.java), GbsGroupRepositoryCustom {

    private val gbsGroup = QGbsGroup.gbsGroup

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
} 