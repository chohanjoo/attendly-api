package com.attendly.domain.repository

import com.attendly.domain.entity.Department
import com.attendly.enums.Role
import com.attendly.domain.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {
    fun findByEmail(email: String): Optional<User>
    fun findByRole(role: Role): List<User>
    fun findByDepartmentAndRole(department: Department, role: Role): List<User>
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<User>
}

interface UserRepositoryCustom {
    fun findByFilters(name: String?, departmentId: Long?, roles: List<Role>?, pageable: Pageable): Page<User>
} 