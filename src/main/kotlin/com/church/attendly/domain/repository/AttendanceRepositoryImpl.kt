package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.Attendance
import com.church.attendly.domain.entity.QAttendance
import com.church.attendly.domain.entity.QGbsGroup
import com.church.attendly.domain.entity.QUser
import com.church.attendly.domain.entity.WorshipStatus
import com.querydsl.jpa.impl.JPAQueryFactory
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
                attendance.weekStart.eq(weekStart),
                attendance.worship.eq(WorshipStatus.O)
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
} 