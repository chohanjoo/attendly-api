package com.attendly.domain.repository

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.Village
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VillageRepository : JpaRepository<Village, Long> {
    fun findByName(name: String): Village?
    fun findByDepartment(department: Department): List<Village>
} 