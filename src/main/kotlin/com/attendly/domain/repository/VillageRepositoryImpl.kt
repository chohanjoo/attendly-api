package com.attendly.domain.repository

import com.attendly.domain.entity.QDepartment
import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.QVillage
import com.attendly.domain.entity.QVillageLeader
import com.attendly.domain.entity.Village
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
    private val department = QDepartment.department

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
    
    override fun findVillagesWithParams(departmentId: Long?, name: String?, pageable: Pageable): Page<Village> {
        val predicate = BooleanBuilder()
        
        departmentId?.let {
            predicate.and(village.department.id.eq(it))
        }
        
        name?.let {
            if (it.isNotBlank()) {
                predicate.and(village.name.containsIgnoreCase(it))
            }
        }
        
        val query = queryFactory
            .selectFrom(village)
            .leftJoin(village.department, department).fetchJoin()
            .leftJoin(village.villageLeader, villageLeader).fetchJoin()
            .where(predicate)
        
        // Count query for total results
        val totalCount = queryFactory
            .select(village.count())
            .from(village)
            .where(predicate)
            .fetchOne() ?: 0L
        
        // Apply pagination
        val results = query
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(village.name.asc())
            .fetch()
        
        return PageImpl(results, pageable, totalCount)
    }
} 