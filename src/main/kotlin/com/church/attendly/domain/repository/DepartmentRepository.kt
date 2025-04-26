package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DepartmentRepository : JpaRepository<Department, Long> {
    fun findByName(name: String): Department?
} 