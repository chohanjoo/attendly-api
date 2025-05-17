package com.attendly.domain.repository

import com.attendly.domain.entity.User
import com.attendly.enums.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserRepositoryCustom {
    fun findByFilters(name: String?, departmentId: Long?, roles: List<Role>?, pageable: Pageable): Page<User>
}