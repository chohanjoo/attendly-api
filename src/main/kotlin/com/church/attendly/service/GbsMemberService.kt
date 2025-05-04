package com.church.attendly.service

import com.church.attendly.api.dto.GbsMembersListResponse
import com.church.attendly.api.dto.LeaderGbsHistoryListResponse
import com.church.attendly.api.dto.LeaderGbsHistoryResponse
import com.church.attendly.api.dto.LeaderGbsResponse
import com.church.attendly.domain.entity.GbsLeaderHistory
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
    private val leaderDelegationRepository: LeaderDelegationRepository,
    private val userService: UserService
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
    
    /**
     * 리더가 지금까지 참여했던 GBS 히스토리 리스트를 조회합니다.
     * 각 GBS 히스토리에는 리더가 언제부터 언제까지 참여했는지에 대한 정보와 해당 GBS 내의 조원들에 대한 정보도 포함됩니다.
     */
    @Transactional(readOnly = true)
    fun getLeaderGbsHistories(leaderId: Long, currentUser: User): LeaderGbsHistoryListResponse {
        // 자신이거나 관리자/교역자/마을장만 조회 가능
        if (currentUser.id != leaderId && 
            currentUser.role != Role.ADMIN && 
            currentUser.role != Role.MINISTER && 
            (currentUser.role != Role.VILLAGE_LEADER || currentUser.villageLeader == null)) {
                throw AccessDeniedException("다른 리더의 히스토리를 조회할 권한이 없습니다.")
        }
        
        // 사용자 정보 조회
        val leader = if (currentUser.id == leaderId) {
            currentUser
        } else {
            userService.findById(leaderId).orElseThrow { ResourceNotFoundException("해당 리더를 찾을 수 없습니다: $leaderId") }
        }
        
        // fetch join을 통해 연관 엔티티를 함께 로딩
        val leaderHistories: List<GbsLeaderHistory> = gbsLeaderHistoryRepository.findByLeaderIdWithDetailsOrderByStartDateDesc(leaderId)
        
        // 각 히스토리에 대한 상세 정보 매핑
        val historyResponses = leaderHistories.map { history: GbsLeaderHistory ->
            val isActive = history.endDate == null
            val membersResponse = if (isActive) {
                // 현재 활성화된 GBS라면 현재 멤버 조회
                organizationService.getGbsMembers(history.gbsGroup.id!!)
            } else {
                // 과거 GBS라면 해당 기간의 멤버 조회
                val endDate = history.endDate ?: LocalDate.now()
                organizationService.getGbsMembers(history.gbsGroup.id!!, endDate)
            }
            
            LeaderGbsHistoryResponse(
                historyId = history.id!!,
                gbsId = history.gbsGroup.id!!,
                gbsName = history.gbsGroup.name,
                villageId = history.gbsGroup.village.id!!,
                villageName = history.gbsGroup.village.name,
                startDate = history.startDate,
                endDate = history.endDate,
                isActive = isActive,
                members = membersResponse.members
            )
        }
        
        return LeaderGbsHistoryListResponse(
            leaderId = leaderId,
            leaderName = leader.name,
            historyCount = historyResponses.size,
            histories = historyResponses
        )
    }
} 