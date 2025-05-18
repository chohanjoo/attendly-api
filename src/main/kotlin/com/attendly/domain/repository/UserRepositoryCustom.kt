package com.attendly.domain.repository

import com.attendly.domain.entity.User
import com.attendly.domain.model.UserFilterDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    fun findByFilters(filter: UserFilterDto, pageable: Pageable): Page<User>

    fun findByFilters(filter: UserFilterDto): List<User>
}