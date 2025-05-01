package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.QSystemLog
import com.church.attendly.domain.entity.SystemLog
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * SystemLogRepositoryCustom 구현체
 * 
 * 주의: QueryDSL Q 클래스 자동생성이 필요합니다.
 * 이 클래스는 임시 구현으로, 실제로는 gradle kapt 태스크를 실행하여 
 * QSystemLog 클래스를 생성해야 합니다.
 */
class SystemLogRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : SystemLogRepositoryCustom {

    override fun searchLogs(
        level: String?,
        category: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        userId: Long?,
        keyword: String?,
        pageable: Pageable
    ): Page<SystemLog> {
        val systemLog = QSystemLog.systemLog
        
        val builder = BooleanBuilder()
        
        // 검색 조건 추가
        level?.let { builder.and(systemLog.level.eq(it)) }
        category?.let { builder.and(systemLog.category.eq(it)) }
        
        if (startTime != null && endTime != null) {
            builder.and(systemLog.timestamp.between(startTime, endTime))
        } else if (startTime != null) {
            builder.and(systemLog.timestamp.goe(startTime))
        } else if (endTime != null) {
            builder.and(systemLog.timestamp.loe(endTime))
        }
        
        userId?.let { builder.and(systemLog.userId.eq(it)) }
        keyword?.let { builder.and(systemLog.message.containsIgnoreCase(it)) }
        
        // 총 개수 쿼리
        val total = queryFactory
            .selectFrom(systemLog)
            .where(builder)
            .fetchCount()
        
        // 실제 데이터 쿼리
        val results = queryFactory
            .selectFrom(systemLog)
            .where(builder)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(systemLog.timestamp.desc())
            .fetch()
        
        return PageImpl(results, pageable, total)
    }
} 