package com.attendly.domain.repository

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.enums.Role
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, Long>, UserRepositoryCustom {
    fun findByEmail(email: String): Optional<User>
    fun findByRole(role: Role): List<User>
    fun findByDepartmentAndRole(department: Department, role: Role): List<User>
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<User>
    fun countByDepartment(department: Department): Int

    /**
     * 특정 마을에 속한 멤버들을 조회합니다.
     */
    fun findByVillage(village: Village): List<User>

    fun findByVillageId(villageId: Long): List<User>
}
