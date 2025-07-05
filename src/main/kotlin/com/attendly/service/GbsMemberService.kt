package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.entity.User
import com.attendly.domain.model.UserFilterDto
import com.attendly.domain.repository.*
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GbsMemberService(
    private val organizationService: OrganizationService,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val leaderDelegationRepository: LeaderDelegationRepository,
    private val userService: UserService,
    private val villageRepository: VillageRepository,
    private val gbsGroupRepository: GbsGroupRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository,
    private val userRepository: UserRepository
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
        
        throw AttendlyApiException(ErrorMessage.ACCESS_DENIED_GBS)
    }
    
    /**
     * 로그인한 리더가 속한 GBS 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getGbsForLeader(userId: Long): LeaderGbsResponse {
        // 리더가 현재 담당하는 GBS 조회
        val leaderHistory = gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId)
            ?: throw AttendlyApiException(ErrorMessage.NO_CURRENT_GBS_FOR_LEADER)
        
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
     * 
     * 최적화된 버전: N+1 문제 해결
     * - 기존: 1 + N개의 쿼리 (N은 리더 히스토리 개수)
     * - 개선: 1개의 복잡한 조인 쿼리
     */
    @Transactional(readOnly = true)
    fun getLeaderGbsHistories(request: LeaderGbsHistoryRequestDto): LeaderGbsHistoryListResponse {
        validateLeaderHistoryAccessPermission(request)
        return buildLeaderGbsHistoryResponse(request)
    }
    
    /**
     * 리더 히스토리 조회 권한을 검증합니다.
     */
    private fun validateLeaderHistoryAccessPermission(request: LeaderGbsHistoryRequestDto) {
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
     * 리더 GBS 히스토리 응답을 생성합니다.
     */
    private fun buildLeaderGbsHistoryResponse(request: LeaderGbsHistoryRequestDto): LeaderGbsHistoryListResponse {
        // 사용자 정보 조회
        val leader = if (request.currentUser.id == request.leaderId) {
            request.currentUser
        } else {
            userService.findById(request.leaderId).orElseThrow { 
                AttendlyApiException(ErrorMessage.LEADER_NOT_FOUND, request.leaderId) 
            }
        }
        
        // Querydsl을 사용하여 리더 히스토리와 멤버 정보를 한 번에 조회
        val historyMemberDtos = gbsLeaderHistoryRepository.findLeaderGbsHistoriesWithMembers(request.leaderId)
        
        // 히스토리별로 그룹화
        val groupedByHistory = historyMemberDtos.groupBy { it.historyId }
        
        // 각 히스토리에 대한 상세 정보 매핑
        val historyResponses = groupedByHistory.map { (historyId, memberDtos) ->
            val firstDto = memberDtos.first()
            
            // 멤버 정보 매핑 (null 값은 제외)
            val members = memberDtos.mapNotNull { dto ->
                if (dto.memberId != null) {
                    GbsMemberResponse(
                        id = dto.memberId,
                        name = dto.memberName!!,
                        email = dto.memberEmail!!,
                        birthDate = dto.memberBirthDate,
                        joinDate = (dto.memberJoinDate ?: firstDto.startDate) as LocalDate,
                        phoneNumber = dto.memberPhoneNumber
                    )
                } else null
            }
            
            LeaderGbsHistoryResponse(
                historyId = historyId,
                gbsId = firstDto.gbsId,
                gbsName = firstDto.gbsName,
                villageId = firstDto.villageId,
                villageName = firstDto.villageName,
                startDate = firstDto.startDate,
                endDate = firstDto.endDate,
                isActive = firstDto.isActive,
                members = members
            )
        }.sortedByDescending { it.startDate }
        
        return LeaderGbsHistoryListResponse(
            leaderId = request.leaderId,
            leaderName = leader.name,
            historyCount = historyResponses.size,
            histories = historyResponses
        )
    }

    /**
     * GBS 배치 정보를 칸반 보드 형태로 조회합니다.
     *
     * @param villageId 마을 ID
     * @param date 조회 기준 날짜
     * @return GBS 배치 정보
     */
    @Transactional(readOnly = true)
    fun getGbsAssignment(villageId: Long, date: LocalDate): GbsAssignmentResponse {
        // 마을 정보 조회
        val village = villageRepository.findById(villageId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                ) 
            }
        
        // 해당 마을의 GBS 그룹 조회
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
        
        // GBS 라벨 정보 생성
        val labels = gbsGroups.map { gbs ->
            KanbanLabel(
                id = gbs.id!!,
                name = gbs.name,
                color = getColorForGbs(gbs.id)
            )
        }
        
        // 칸반 컬럼 생성
        val columns = mutableListOf<KanbanColumn>()
        
        // 미배정 멤버 컬럼
        val unassignedMembers = getUsersNotAssignedToGbs(villageId, date)
        val unassignedCards = unassignedMembers.map { user ->
            KanbanCard(
                id = "user-${user.id}",
                content = user.name,
                labels = emptyList()
            )
        }
        columns.add(
            KanbanColumn(
                id = "unassigned",
                title = "미배정 멤버",
                cards = unassignedCards
            )
        )
        
        // 리더 컬럼
        val leaderCards = mutableListOf<KanbanCard>()
        gbsGroups.forEach { gbs ->
            val currentLeaderHistory = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbs.id!!, date)
            if (currentLeaderHistory != null) {
                val leader = currentLeaderHistory.leader
                val card = KanbanCard(
                    id = "user-${leader.id}",
                    content = leader.name,
                    labels = listOf(labels.find { it.id == gbs.id }!!)
                )
                leaderCards.add(card)
            }
        }
        columns.add(KanbanColumn(
            id = "leaders",
            title = "리더",
            cards = leaderCards
        ))
        
        // 배정 완료 컬럼
        val assignedCards = mutableListOf<KanbanCard>()
        gbsGroups.forEach { gbs ->
            val memberHistories = gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbs.id!!, date)
            memberHistories.forEach { history ->
                val member = history.member
                // 이미 리더로 배정된 사용자는 제외
                if (!leaderCards.any { it.id == "user-${member.id}" }) {
                    val card = KanbanCard(
                        id = "user-${member.id}",
                        content = member.name,
                        labels = listOf(labels.find { it.id == gbs.id }!!)
                    )
                    assignedCards.add(card)
                }
            }
        }
        columns.add(KanbanColumn(
            id = "assigned",
            title = "배정 완료",
            cards = assignedCards
        ))
        
        return GbsAssignmentResponse(
            villageId = villageId,
            villageName = village.name,
            columns = columns,
            labels = labels
        )
    }
    
    /**
     * GBS에 배정되지 않은 사용자 목록을 조회합니다.
     */
    private fun getUsersNotAssignedToGbs(villageId: Long, date: LocalDate): List<User> {
        // 해당 마을에 소속된 모든 사용자 조회
        val allVillageUsers = userRepository.findByVillageId(villageId)
        
        // 현재 GBS에 배정된 사용자 ID 목록
        val assignedUserIds = mutableSetOf<Long>()
        
        // 리더로 배정된 사용자 ID 추가
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
        gbsGroups.forEach { gbs ->
            val leaderHistory = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbs.id!!, date)
            if (leaderHistory != null) {
                assignedUserIds.add(leaderHistory.leader.id!!)
            }
            
            // 멤버로 배정된 사용자 ID 추가
            val memberHistories = gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbs.id, date)
            memberHistories.forEach { history ->
                assignedUserIds.add(history.member.id!!)
            }
        }
        
        // 배정되지 않은 사용자만 필터링
        return allVillageUsers.filter { user -> !assignedUserIds.contains(user.id) }
    }
    
    /**
     * GBS ID에 따라 고유한 색상을 반환합니다.
     */
    private fun getColorForGbs(gbsId: Long?): String {
        // 미리 정의된 색상 리스트
        val colors = listOf(
            "#FF5733", "#33FF57", "#3357FF", "#FF33A8", "#A833FF",
            "#33FFF3", "#FFD433", "#FF8033", "#33FF98", "#3380FF"
        )
        // GBS ID를 기준으로 색상 할당
        return if (gbsId != null) {
            val index = ((gbsId % colors.size) + colors.size) % colors.size
            colors[index.toInt()]
        } else {
            "#CCCCCC" // 기본 색상
        }
    }

    /**
     * GBS 배치 정보를 저장합니다.
     *
     * @param villageId 마을 ID
     * @param request GBS 배치 정보 저장 요청
     * @return GBS 배치 정보 저장 응답
     */
    @Transactional
    fun saveGbsAssignment(villageId: Long, request: GbsAssignmentSaveRequest): GbsAssignmentSaveResponse {
        // 마을 정보 확인
        val village = villageRepository.findById(villageId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                ) 
            }
        
        var totalMemberCount = 0
        
        // 각 GBS 그룹에 대한 배치 정보 저장
        request.assignments.forEach { assignment ->
            // GBS 그룹 확인
            val gbsGroup = gbsGroupRepository.findById(assignment.gbsId)
                .orElseThrow { 
                    AttendlyApiException(
                        ErrorMessage.GBS_GROUP_NOT_FOUND, 
                        ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, assignment.gbsId)
                    ) 
                }
            
            // 마을에 속하는 GBS인지 확인
            if (gbsGroup.village.id != villageId) {
                throw AttendlyApiException(
                    ErrorMessage.GBS_GROUP_NOT_IN_VILLAGE,
                    "GBS ID: ${assignment.gbsId}, Village ID: $villageId"
                )
            }
            
            // 리더 확인
            val leader = userRepository.findById(assignment.leaderId)
                .orElseThrow { 
                    AttendlyApiException(
                        ErrorMessage.USER_NOT_FOUND, 
                        ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, assignment.leaderId)
                    ) 
                }
            
            // 기존 활성 리더 기록 조회 및 종료
            val currentLeaderHistory = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(assignment.gbsId, request.startDate)
            if (currentLeaderHistory != null && currentLeaderHistory.leader.id != assignment.leaderId) {
                // 기존 리더와 새로운 리더가 다른 경우 기존 기록 종료
                // 새 객체 생성하여 endDate 설정 후 저장
                val updatedLeaderHistory = GbsLeaderHistory(
                    id = currentLeaderHistory.id,
                    gbsGroup = currentLeaderHistory.gbsGroup,
                    leader = currentLeaderHistory.leader,
                    startDate = currentLeaderHistory.startDate,
                    endDate = request.startDate.minusDays(1)
                )
                gbsLeaderHistoryRepository.save(updatedLeaderHistory)
            }
            
            // 새로운 리더가 기존 리더와 다르거나 기존 리더 기록이 없는 경우 새 기록 생성
            if (currentLeaderHistory == null || currentLeaderHistory.leader.id != assignment.leaderId) {
                val newLeaderHistory = GbsLeaderHistory(
                    gbsGroup = gbsGroup,
                    leader = leader,
                    startDate = request.startDate,
                    endDate = null
                )
                gbsLeaderHistoryRepository.save(newLeaderHistory)
            }
            
            // 기존 멤버 기록 조회 및 종료
            val currentMemberHistories = gbsMemberHistoryRepository.findCurrentMembersByGbsId(assignment.gbsId, request.startDate)
            val currentMemberIds = currentMemberHistories.map { it.member.id!! }.toSet()
            val newMemberIds = assignment.memberIds.toSet()
            
            // 제거된 멤버들의 기록 종료
            val removedMemberHistories = currentMemberHistories.filter { it.member.id!! !in newMemberIds }
            removedMemberHistories.forEach { history ->
                // 새 객체 생성하여 endDate 설정 후 저장
                val updatedHistory = GbsMemberHistory(
                    id = history.id,
                    gbsGroup = history.gbsGroup,
                    member = history.member, 
                    startDate = history.startDate,
                    endDate = request.startDate.minusDays(1)
                )
                gbsMemberHistoryRepository.save(updatedHistory)
            }
            
            // 새로 추가된 멤버들의 기록 생성
            val addedMemberIds = newMemberIds - currentMemberIds
            addedMemberIds.forEach { memberId ->
                val member = userRepository.findById(memberId)
                    .orElseThrow { 
                        AttendlyApiException(
                            ErrorMessage.USER_NOT_FOUND, 
                            ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, memberId)
                        ) 
                    }
                
                val newMemberHistory = GbsMemberHistory(
                    gbsGroup = gbsGroup,
                    member = member,
                    startDate = request.startDate,
                    endDate = null
                )
                gbsMemberHistoryRepository.save(newMemberHistory)
            }
            
            // 멤버 수 카운팅 (리더 + 멤버)
            totalMemberCount += 1 + assignment.memberIds.size
        }
        
        return GbsAssignmentSaveResponse(
            villageId = villageId,
            assignmentCount = request.assignments.size,
            memberCount = totalMemberCount,
            message = "GBS 배치가 성공적으로 저장되었습니다."
        )
    }

    /**
     * GBS 리더로 지정할 수 있는 사용자 목록을 조회합니다.
     *
     * @param villageId 마을 ID
     * @return 리더 후보 목록 응답
     */
    @Transactional(readOnly = true)
    fun getLeaderCandidates(villageId: Long): List<LeaderCandidate> {
        // 마을 정보 확인
        val village = villageRepository.findById(villageId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.VILLAGE_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
                ) 
            }
        
        // 해당 마을에 소속된 사용자 중 리더 후보를 조회
        val villageUsers = userRepository.findByFilters(
            UserFilterDto(
                villageId = villageId,
                roles = listOf(Role.LEADER),
            )
        )
        
        // 각 사용자별 이전 GBS 리더 경험 횟수 조회
        val candidates = villageUsers.map { user ->
            val previousGbsHistories = gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(user.id!!)
            val isCurrentlyLeader = gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(user.id) != null
            
            LeaderCandidate(
                id = user.id,
                name = user.name,
                email = user.email,
                isLeader = isCurrentlyLeader,
                previousGbsCount = previousGbsHistories.size
            )
        }
        
        // 현재 리더인 사용자와 이전 경험이 많은 사용자를 우선 순위로 정렬
        return candidates.sortedWith(
            compareByDescending<LeaderCandidate> { it.isLeader }
                .thenByDescending { it.previousGbsCount }
        )
    }
} 