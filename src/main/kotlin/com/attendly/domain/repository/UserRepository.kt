package com.attendly.domain.repository

import com.attendly.domain.entity.Department
import com.attendly.enums.Role
import com.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByRole(role: Role): List<User>
    fun findByDepartmentAndRole(department: Department, role: Role): List<User>
} 