package com.church.attendly.service

import com.church.attendly.api.dto.GbsMembersListResponse
import com.church.attendly.api.dto.LeaderGbsResponse
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.LeaderDelegationRepository
import com.church.attendly.exception.AccessDeniedException
import com.church.attendly.exception.ResourceNotFoundException
import com.church.attendly.service.OrganizationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GbsMemberService(
    private val organizationService: OrganizationService,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val leaderDelegationRepository: LeaderDelegationRepository
) {

    /**
     * GBS 멤버 정보를 조회합니다. 
     * 현재 사용자의 권한에 따라 접근 가능 여부를 검증합니다.
     */
    @Transactional(readOnly = true)
    fun getGbsMembers(gbsId: Long, targetDate: LocalDate, currentUser: User): GbsMembersListResponse {
        validateGbsAccessPermission(gbsId, targetDate, currentUser)
        return organizationService.getGbsMembers(gbsId, targetDate)
    }
    
    /**
     * 현재 사용자가 특정 GBS의 멤버 정보를 조회할 권한이 있는지 검증합니다.
     */
    private fun validateGbsAccessPermission(gbsId: Long, targetDate: LocalDate, currentUser: User) {
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
            // 자신이 리더인지 확인
            val isLeader = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, currentUser.id!!) != null
            
            // 위임받았는지 확인
            val isDelegated = leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, currentUser.id, targetDate) != null
            
            if (isLeader || isDelegated) {
                return
            }
        }
        
        throw AccessDeniedException("이 GBS의 멤버 정보를 조회할 권한이 없습니다.")
    }
    
    /**
     * 로그인한 리더가 속한 GBS 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getGbsForLeader(userId: Long): LeaderGbsResponse {
        // 리더가 현재 담당하는 GBS 조회
        val leaderHistory = gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId)
            ?: throw ResourceNotFoundException("현재 담당하는 GBS가 없습니다")
        
        val gbs = leaderHistory.gbsGroup
        
        return LeaderGbsResponse(
            gbsId = gbs.id!!,
            gbsName = gbs.name,
            villageId = gbs.village.id!!,
            villageName = gbs.village.name,
            leaderId = leaderHistory.leader.id!!,
            leaderName = leaderHistory.leader.name,
            startDate = leaderHistory.startDate
        )
    }
} 