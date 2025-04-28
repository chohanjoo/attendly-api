package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.QGbsGroup
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

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
} 