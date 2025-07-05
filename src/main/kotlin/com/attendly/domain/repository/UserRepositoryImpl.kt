package com.attendly.domain.repository

import com.attendly.domain.entity.QUser.user
import com.attendly.domain.entity.User
import com.attendly.domain.model.UserFilterDto
import com.attendly.enums.Role
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryImpl : UserRepositoryCustom {
    
    @PersistenceContext
    private lateinit var entityManager: EntityManager
    
    override fun findByFilters(filter: UserFilterDto, pageable: Pageable): Page<User> {
        val queryFactory = JPAQueryFactory(entityManager)

        val predicate = BooleanBuilder()

        addWhereCondition(filter, predicate)

        // 조회 쿼리
        val query = queryFactory
            .selectFrom(user)
            .where(predicate)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
        
        // 정렬 적용
        pageable.sort.forEach { sort ->
            val path = sort.property
            if (sort.isAscending) {
                when (path) {
                    "id" -> query.orderBy(user.id.asc())
                    "name" -> query.orderBy(user.name.asc())
                    "createdAt" -> query.orderBy(user.createdAt.asc())
                    else -> query.orderBy(user.id.asc())
                }
            } else {
                when (path) {
                    "id" -> query.orderBy(user.id.desc())
                    "name" -> query.orderBy(user.name.desc())
                    "createdAt" -> query.orderBy(user.createdAt.desc())
                    else -> query.orderBy(user.id.desc())
                }
            }
        }
        
        // 결과 조회
        val content = query.fetch()
        
        // 카운트 쿼리
        val countQuery: JPAQuery<Long> = queryFactory
            .select(user.count())
            .from(user)
            .where(predicate)
        
        // 전체 개수 조회
        val totalCount = countQuery.fetchOne() ?: 0L
        
        // Page 객체 반환
        return PageImpl(content, pageable, totalCount)
    }

    /**
     * Map을 이용한 필터 조건으로 사용자를 조회합니다.
     */
    override fun findByFilters(filter: UserFilterDto): List<User> {
        val queryFactory = JPAQueryFactory(entityManager)

        val predicate = BooleanBuilder()

        addWhereCondition(filter, predicate)
        
        // 조회 쿼리 실행 및 결과 반환
        return queryFactory
            .selectFrom(user)
            .where(predicate)
            .orderBy(user.id.asc())
            .fetch()
    }

    override fun findByRoles(roles: List<Role>): List<User> {
        if (roles.isEmpty()) {
            return emptyList()
        }
        
        val queryFactory = JPAQueryFactory(entityManager)
        
        return queryFactory
            .selectFrom(user)
            .where(user.role.`in`(roles))
            .orderBy(user.id.asc())
            .fetch()
    }

    private fun addWhereCondition(
        filter: UserFilterDto,
        predicate: BooleanBuilder
    ) {
        // 이름 필터링
        if (!filter.name.isNullOrBlank()) {
            predicate.and(user.name.containsIgnoreCase(filter.name))
        }

        // 부서 ID 필터링
        if (filter.departmentId != null) {
            predicate.and(user.department.id.eq(filter.departmentId))
        }

        // 마을 ID 필터링
        if (filter.villageId != null) {
            predicate.and(user.village.id.eq(filter.villageId))
        }

        // 역할 필터링
        if (!filter.roles.isNullOrEmpty()) {
            predicate.and(user.role.`in`(filter.roles))
        }
    }
} 