package com.attendly.domain.repository

import com.attendly.domain.entity.Attendance
import com.attendly.domain.entity.QAttendance
import com.attendly.domain.entity.QGbsGroup
import com.attendly.domain.entity.QUser
import com.attendly.enums.AttendanceStatus
import com.attendly.enums.MinistryStatus
import com.attendly.enums.WorshipStatus
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport
import java.time.LocalDate

class AttendanceRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : QuerydslRepositorySupport(Attendance::class.java), AttendanceRepositoryCustom {

    private val attendance = QAttendance.attendance
    private val member = QUser.user
    private val gbsGroup = QGbsGroup.gbsGroup

    override fun countAttendancesByGbsIdsAndWeek(gbsIds: List<Long>, weekStart: LocalDate): Long {
        return queryFactory
            .select(attendance.count())
            .from(attendance)
            .where(
                attendance.gbsGroup.id.`in`(gbsIds),
                attendance.weekStart.eq(weekStart)
            )
            .fetchOne() ?: 0L
    }

    override fun findDetailsByGbsIdAndWeek(gbsId: Long, weekStart: LocalDate): List<Attendance> {
        return queryFactory
            .selectFrom(attendance)
            .join(attendance.member, member).fetchJoin()
            .where(
                attendance.gbsGroup.id.eq(gbsId),
                attendance.weekStart.eq(weekStart)
            )
            .fetch()
    }
    
    override fun findDetailsByGbsIdAndWeeks(gbsId: Long, weekStarts: List<LocalDate>): Map<LocalDate, List<Attendance>> {
        if (weekStarts.isEmpty()) {
            return emptyMap()
        }
        
        val attendances = queryFactory
            .selectFrom(attendance)
            .where(
                attendance.gbsGroup.id.eq(gbsId),
                attendance.weekStart.`in`(weekStarts)
            )
            .fetch()
            
        return attendances.groupBy { it.weekStart }
    }

    override fun findByVillageIdAndWeek(villageId: Long, weekStart: LocalDate): List<Attendance> {
        return queryFactory
            .selectFrom(attendance)
            .join(attendance.member, member).fetchJoin()
            .join(attendance.gbsGroup, gbsGroup).fetchJoin()
            .where(
                gbsGroup.village.id.eq(villageId),
                attendance.weekStart.eq(weekStart)
            )
            .fetch()
    }
    
    override fun findByVillageIdAndDateRange(villageId: Long, startDate: LocalDate, endDate: LocalDate): List<Attendance> {
        val weekStarts = mutableListOf<LocalDate>()
        var currentWeekStart = startDate
        
        // 날짜 범위 내의 모든 주의 시작일 계산
        while (!currentWeekStart.isAfter(endDate)) {
            weekStarts.add(currentWeekStart)
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }
        
        if (weekStarts.isEmpty()) {
            return emptyList()
        }
        
        return queryFactory
            .selectFrom(attendance)
            .join(attendance.member, member).fetchJoin()
            .join(attendance.gbsGroup, gbsGroup).fetchJoin()
            .where(
                gbsGroup.village.id.eq(villageId),
                attendance.weekStart.`in`(weekStarts)
            )
            .fetch()
    }
    
    override fun findAttendancesForAdmin(
        search: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        status: AttendanceStatus?,
        pageable: Pageable
    ): Page<Attendance> {
        val whereClause = BooleanBuilder()
        
        // 검색어 조건: 사용자 이름 검색
        search?.let { 
            if (it.isNotBlank()) {
                whereClause.and(member.name.containsIgnoreCase(it))
            }
        }
        
        // 날짜 범위 조건
        startDate?.let { whereClause.and(attendance.weekStart.goe(it)) }
        endDate?.let { whereClause.and(attendance.weekStart.loe(it)) }
        
        // 출석 상태 조건
        status?.let {
            when (status) {
                AttendanceStatus.PRESENT -> whereClause.and(
                    attendance.worship.eq(WorshipStatus.O)
                        .and(attendance.ministry.eq(MinistryStatus.A))
                )
                AttendanceStatus.LATE -> whereClause.and(
                    attendance.worship.eq(WorshipStatus.O)
                        .and(attendance.ministry.eq(MinistryStatus.B))
                )
                AttendanceStatus.EXCUSED -> whereClause.and(
                    attendance.worship.eq(WorshipStatus.X)
                        .and(attendance.qtCount.gt(0))
                )
                AttendanceStatus.ABSENT -> whereClause.and(
                    attendance.worship.eq(WorshipStatus.X)
                        .and(attendance.qtCount.eq(0))
                )
            }
        }
        
        // 총 개수 조회
        val totalCount = queryFactory
            .select(attendance.count())
            .from(attendance)
            .join(attendance.member, member)
            .where(whereClause)
            .fetchOne() ?: 0L
        
        // 페이지 조회
        val results = queryFactory
            .selectFrom(attendance)
            .join(attendance.member, member).fetchJoin()
            .join(attendance.gbsGroup, gbsGroup).fetchJoin()
            .where(whereClause)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(attendance.weekStart.desc())
            .fetch()
        
        return PageImpl(results, pageable, totalCount)
    }
    
    private fun nullSafeEq(expression: BooleanExpression?, value: Any?): BooleanExpression? {
        return if (value == null) null else expression
    }
} 