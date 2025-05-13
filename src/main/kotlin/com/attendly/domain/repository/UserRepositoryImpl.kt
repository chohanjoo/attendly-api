package com.attendly.domain.repository

import com.attendly.domain.entity.QUser
import com.attendly.domain.entity.User
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
    
    override fun findByFilters(name: String?, departmentId: Long?, roles: List<Role>?, pageable: Pageable): Page<User> {
        val queryFactory = JPAQueryFactory(entityManager)
        val user = QUser.user
        
        val predicate = BooleanBuilder()
        
        // 이름 필터링
        if (!name.isNullOrBlank()) {
            predicate.and(user.name.containsIgnoreCase(name))
        }
        
        // 부서 ID 필터링
        if (departmentId != null) {
            predicate.and(user.department.id.eq(departmentId))
        }
        
        // 역할 필터링
        if (!roles.isNullOrEmpty()) {
            predicate.and(user.role.`in`(roles))
        }
        
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
} 