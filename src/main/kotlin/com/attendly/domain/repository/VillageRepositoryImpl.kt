package com.attendly.domain.repository

import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.QVillage
import com.attendly.domain.entity.QVillageLeader
import com.attendly.domain.entity.Village
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class VillageRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(Village::class.java), VillageRepositoryCustom {

    private val village = QVillage.village
    private val gbsGroup = QGbsGroup.gbsGroup
    private val villageLeader = QVillageLeader.villageLeader

    override fun findVillageWithActiveGbsGroups(villageId: Long, date: LocalDate): Village? {
        return queryFactory
            .selectFrom(village)
            .distinct()
            .leftJoin(village.gbsGroups, gbsGroup).fetchJoin()
            .leftJoin(village.villageLeader, villageLeader).fetchJoin()
            .where(
                village.id.eq(villageId),
                gbsGroup.termStartDate.loe(date),
                gbsGroup.termEndDate.goe(date)
            )
            .fetchOne()
    }
} 