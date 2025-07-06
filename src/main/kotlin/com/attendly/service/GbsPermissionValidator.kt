package com.attendly.service

import com.attendly.api.dto.LeaderGbsHistoryRequestDto
import com.attendly.domain.entity.User
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GbsPermissionValidator(
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val leaderDelegationRepository: LeaderDelegationRepository,
    private val organizationService: OrganizationService
) {
    
    /**
     * 현재 사용자가 특정 GBS의 멤버 정보를 조회할 권한이 있는지 검증합니다.
     */
    fun validateGbsAccessPermission(gbsId: Long, targetDate: LocalDate, currentUser: User) {
        // 관리자나 교역자면 바로 접근 허용
        if (currentUser.role == Role.ADMIN || currentUser.role == Role.MINISTER) {
            return
        }
        
        // 마을장인 경우 해당 GBS가 자신의 마을에 속하는지 확인
        if (currentUser.role == Role.VILLAGE_LEADER && currentUser.villageLeader != null) {
            val gbsGroup = organizationService.getGbsGroupById(gbsId)
            if (gbsGroup.village.id == currentUser.villageLeader.village.id) {
                return
            }
        }
        
        // 리더인 경우 자신이 해당 GBS의 리더인지 또는 위임받았는지 확인
        if (currentUser.role == Role.LEADER) {
            val hasAccess = hasLeaderAccess(gbsId, targetDate, currentUser)
            if (hasAccess) {
                return
            }
        }
        
        throw AttendlyApiException(ErrorMessage.ACCESS_DENIED_GBS)
    }
    
    /**
     * 리더 히스토리 조회 권한을 검증합니다.
     */
    fun validateLeaderHistoryAccessPermission(request: LeaderGbsHistoryRequestDto) {
        val currentUser = request.currentUser
        
        // 자신의 히스토리 조회는 허용
        if (currentUser.id == request.leaderId) return
        
        // 관리자, 교역자는 모든 히스토리 조회 가능
        if (currentUser.role in listOf(Role.ADMIN, Role.MINISTER)) return
        
        // 마을장은 자신의 마을 리더들의 히스토리 조회 가능
        if (currentUser.role == Role.VILLAGE_LEADER && currentUser.villageLeader != null) return
        
        throw AttendlyApiException(ErrorMessage.ACCESS_DENIED_LEADER_HISTORY)
    }
    
    /**
     * 리더가 특정 GBS에 대한 접근 권한이 있는지 확인합니다.
     */
    private fun hasLeaderAccess(gbsId: Long, targetDate: LocalDate, currentUser: User): Boolean {
        // 자신이 리더인지 확인
        val isLeader = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, currentUser.id!!) != null
        
        // 위임받았는지 확인
        val isDelegated = leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, currentUser.id, targetDate) != null
        
        return isLeader || isDelegated
    }
} 