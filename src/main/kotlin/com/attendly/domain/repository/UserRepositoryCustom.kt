package com.attendly.domain.repository

import com.attendly.domain.entity.User
import com.attendly.domain.model.UserFilterDto
import com.attendly.enums.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    fun findByFilters(filter: UserFilterDto, pageable: Pageable): Page<User>

    fun findByFilters(filter: UserFilterDto): List<User>
    
    /**
     * 여러 역할로 사용자를 조회합니다.
     * N+1 문제를 방지하기 위해 단일 쿼리로 조회합니다.
     */
    fun findByRoles(roles: List<Role>): List<User>
}