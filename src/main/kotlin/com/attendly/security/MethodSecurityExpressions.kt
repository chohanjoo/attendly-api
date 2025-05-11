package com.attendly.security

import com.attendly.enums.Role
import com.attendly.domain.repository.VillageRepository
import com.attendly.service.AttendanceService
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("securityUtils")
class MethodSecurityExpressions(
    private val attendanceService: AttendanceService,
    private val villageRepository: VillageRepository
) {

    /**
     * 사용자가 특정 GBS에 대한 출석 입력 권한이 있는지 확인
     */
    fun canManageGbsAttendance(gbsId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = authentication.principal as? UserDetailsAdapter ?: return false
        val user = userDetails.getUser()
        
        // 관리자, 교역자, 마을장은 모든 GBS에 접근 가능
        if (user.role == Role.ADMIN || user.role == Role.MINISTER || user.role == Role.VILLAGE_LEADER) {
            return true
        }
        
        // 리더는 자신의 GBS 또는 위임된 GBS만 접근 가능
        return user.role == Role.LEADER && attendanceService.hasLeaderAccess(gbsId, user.id!!)
    }
    
    /**
     * 사용자가 특정 마을에 접근할 권한이 있는지 확인
     */
    fun canAccessVillage(villageId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = authentication.principal as? UserDetailsAdapter ?: return false
        val user = userDetails.getUser()
        
        // 관리자, 교역자는 모든 마을에 접근 가능
        if (user.role == Role.ADMIN || user.role == Role.MINISTER) {
            return true
        }
        
        // 마을장인 경우 자신의 마을인지 확인
        if (user.role == Role.VILLAGE_LEADER) {
            val villageLeader = user.villageLeader
            return villageLeader != null && villageLeader.village.id == villageId
        }
        
        return false
    }
    
    /**
     * 사용자가 특정 마을의 마을장인지 확인
     */
    fun isVillageLeaderOf(villageId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = authentication.principal as? UserDetailsAdapter ?: return false
        val user = userDetails.getUser()
        
        // 관리자, 교역자는 모든 마을에 접근 가능
        if (user.role == Role.ADMIN || user.role == Role.MINISTER) {
            return true
        }
        
        // 마을장인 경우 자신의 마을인지 확인
        if (user.role == Role.VILLAGE_LEADER) {
            val villageLeader = user.villageLeader
            return villageLeader != null && villageLeader.village.id == villageId
        }
        
        return false
    }
    
    /**
     * 사용자가 교역자인지 확인
     */
    fun isMinister(authentication: Authentication): Boolean {
        val userDetails = authentication.principal as? UserDetailsAdapter ?: return false
        val user = userDetails.getUser()
        return user.role == Role.MINISTER
    }
} 