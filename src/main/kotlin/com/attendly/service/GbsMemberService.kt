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
    private val gbsPermissionValidator: GbsPermissionValidator,
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
    fun getGbsMembers(searchDto: GbsMemberSearchDto): GbsMembersListResponse {
        gbsPermissionValidator.validateGbsAccessPermission(searchDto.gbsId, searchDto.targetDate, searchDto.currentUser)
        return organizationService.getGbsMembers(searchDto.gbsId, searchDto.targetDate)
    }
    
    /**
     * 로그인한 리더가 속한 GBS 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getGbsForLeader(userId: Long): LeaderGbsResponse {
        val leaderHistory = getActiveLeaderHistory(userId)
        return mapToLeaderGbsResponse(leaderHistory)
    }
    
    /**
     * 활성 리더 히스토리를 조회합니다.
     */
    private fun getActiveLeaderHistory(userId: Long): GbsLeaderHistory {
        return gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId)
            ?: throw AttendlyApiException(ErrorMessage.NO_CURRENT_GBS_FOR_LEADER)
    }
    
    /**
     * GbsLeaderHistory를 LeaderGbsResponse로 변환합니다.
     */
    private fun mapToLeaderGbsResponse(leaderHistory: GbsLeaderHistory): LeaderGbsResponse {
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
     */
    @Transactional(readOnly = true)
    fun getLeaderGbsHistories(request: LeaderGbsHistoryRequestDto): LeaderGbsHistoryListResponse {
        gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
        return buildLeaderGbsHistoryResponse(request)
    }
    
    /**
     * 리더 GBS 히스토리 응답을 생성합니다.
     */
    private fun buildLeaderGbsHistoryResponse(request: LeaderGbsHistoryRequestDto): LeaderGbsHistoryListResponse {
        val leader = getLeaderForHistory(request)
        val historyMemberDtos = gbsLeaderHistoryRepository.findLeaderGbsHistoriesWithMembers(request.leaderId)
        val historyResponses = mapToLeaderGbsHistoryResponses(historyMemberDtos)
        
        return LeaderGbsHistoryListResponse(
            leaderId = request.leaderId,
            leaderName = leader.name,
            historyCount = historyResponses.size,
            histories = historyResponses
        )
    }
    
    /**
     * 리더 히스토리 조회를 위한 리더 정보를 조회합니다.
     */
    private fun getLeaderForHistory(request: LeaderGbsHistoryRequestDto): User {
        return if (request.currentUser.id == request.leaderId) {
            request.currentUser
        } else {
            userService.findById(request.leaderId).orElseThrow { 
                AttendlyApiException(ErrorMessage.LEADER_NOT_FOUND, request.leaderId) 
            }
        }
    }
    
    /**
     * 히스토리 멤버 DTOs를 LeaderGbsHistoryResponse 리스트로 변환합니다.
     */
    private fun mapToLeaderGbsHistoryResponses(historyMemberDtos: List<LeaderGbsHistoryMemberDto>): List<LeaderGbsHistoryResponse> {
        val groupedByHistory = historyMemberDtos.groupBy { it.historyId }
        
        return groupedByHistory.map { (historyId, memberDtos) ->
            val firstDto = memberDtos.first()
            val members = mapToGbsMemberResponses(memberDtos, firstDto.startDate)
            
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
    }
    
    /**
     * 멤버 DTOs를 GbsMemberResponse 리스트로 변환합니다.
     */
    private fun mapToGbsMemberResponses(memberDtos: List<LeaderGbsHistoryMemberDto>, historyStartDate: LocalDate): List<GbsMemberResponse> {
        return memberDtos.mapNotNull { dto ->
            if (dto.memberId != null) {
                GbsMemberResponse(
                    id = dto.memberId,
                    name = dto.memberName!!,
                    email = dto.memberEmail!!,
                    birthDate = dto.memberBirthDate,
                    joinDate = (dto.memberJoinDate ?: historyStartDate) as LocalDate,
                    phoneNumber = dto.memberPhoneNumber
                )
            } else null
        }
    }

    /**
     * GBS 배치 정보를 칸반 보드 형태로 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getGbsAssignment(villageId: Long, date: LocalDate): GbsAssignmentResponse {
        val village = getVillageById(villageId)
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
        val labels = createKanbanLabels(gbsGroups)
        val columns = createKanbanColumns(villageId, date, gbsGroups, labels)
        
        return GbsAssignmentResponse(
            villageId = villageId,
            villageName = village.name,
            columns = columns,
            labels = labels
        )
    }
    
    /**
     * 마을 ID로 마을 정보를 조회합니다.
     */
    private fun getVillageById(villageId: Long) = villageRepository.findById(villageId)
        .orElseThrow { 
            AttendlyApiException(
                ErrorMessage.VILLAGE_NOT_FOUND, 
                ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)
            ) 
        }
    
    /**
     * 칸반 라벨을 생성합니다.
     */
    private fun createKanbanLabels(gbsGroups: List<com.attendly.domain.entity.GbsGroup>): List<KanbanLabel> {
        return gbsGroups.map { gbs ->
            KanbanLabel(
                id = gbs.id!!,
                name = gbs.name,
                color = getColorForGbs(gbs.id)
            )
        }
    }
    
    /**
     * 칸반 컬럼을 생성합니다.
     */
    private fun createKanbanColumns(
        villageId: Long,
        date: LocalDate,
        gbsGroups: List<com.attendly.domain.entity.GbsGroup>,
        labels: List<KanbanLabel>
    ): List<KanbanColumn> {
        val columns = mutableListOf<KanbanColumn>()
        
        // 미배정 멤버 컬럼
        columns.add(createUnassignedColumn(villageId, date))
        
        // 리더 컬럼
        columns.add(createLeaderColumn(gbsGroups, labels, date))
        
        // 배정 완료 컬럼
        columns.add(createAssignedColumn(gbsGroups, labels, date))
        
        return columns
    }
    
    /**
     * 미배정 멤버 컬럼을 생성합니다.
     */
    private fun createUnassignedColumn(villageId: Long, date: LocalDate): KanbanColumn {
        val unassignedMembers = getUsersNotAssignedToGbs(villageId, date)
        val unassignedCards = unassignedMembers.map { user ->
            KanbanCard(
                id = "user-${user.id}",
                content = user.name,
                labels = emptyList()
            )
        }
        return KanbanColumn(
            id = "unassigned",
            title = "미배정 멤버",
            cards = unassignedCards
        )
    }
    
    /**
     * 리더 컬럼을 생성합니다.
     */
    private fun createLeaderColumn(
        gbsGroups: List<com.attendly.domain.entity.GbsGroup>,
        labels: List<KanbanLabel>,
        date: LocalDate
    ): KanbanColumn {
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
        return KanbanColumn(
            id = "leaders",
            title = "리더",
            cards = leaderCards
        )
    }
    
    /**
     * 배정 완료 컬럼을 생성합니다.
     */
    private fun createAssignedColumn(
        gbsGroups: List<com.attendly.domain.entity.GbsGroup>,
        labels: List<KanbanLabel>,
        date: LocalDate
    ): KanbanColumn {
        val assignedCards = mutableListOf<KanbanCard>()
        val leaderUserIds = getLeaderUserIds(gbsGroups, date)
        
        gbsGroups.forEach { gbs ->
            val memberHistories = gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbs.id!!, date)
            memberHistories.forEach { history ->
                val member = history.member
                // 이미 리더로 배정된 사용자는 제외
                if (!leaderUserIds.contains(member.id)) {
                    val card = KanbanCard(
                        id = "user-${member.id}",
                        content = member.name,
                        labels = listOf(labels.find { it.id == gbs.id }!!)
                    )
                    assignedCards.add(card)
                }
            }
        }
        return KanbanColumn(
            id = "assigned",
            title = "배정 완료",
            cards = assignedCards
        )
    }
    
    /**
     * 리더로 배정된 사용자 ID 목록을 조회합니다.
     */
    private fun getLeaderUserIds(gbsGroups: List<com.attendly.domain.entity.GbsGroup>, date: LocalDate): Set<Long> {
        return gbsGroups.mapNotNull { gbs ->
            val leaderHistory = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbs.id!!, date)
            leaderHistory?.leader?.id
        }.toSet()
    }
    
    /**
     * GBS에 배정되지 않은 사용자 목록을 조회합니다.
     */
    private fun getUsersNotAssignedToGbs(villageId: Long, date: LocalDate): List<User> {
        val allVillageUsers = userRepository.findByVillageId(villageId)
        val assignedUserIds = getAssignedUserIds(villageId, date)
        return allVillageUsers.filter { user -> !assignedUserIds.contains(user.id) }
    }
    
    /**
     * 배정된 사용자 ID 목록을 조회합니다.
     */
    private fun getAssignedUserIds(villageId: Long, date: LocalDate): Set<Long> {
        val assignedUserIds = mutableSetOf<Long>()
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
        
        gbsGroups.forEach { gbs ->
            // 리더로 배정된 사용자 ID 추가
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
        
        return assignedUserIds
    }
    
    /**
     * GBS ID에 따라 고유한 색상을 반환합니다.
     */
    private fun getColorForGbs(gbsId: Long?): String {
        val colors = listOf(
            "#FF5733", "#33FF57", "#3357FF", "#FF33A8", "#A833FF",
            "#33FFF3", "#FFD433", "#FF8033", "#33FF98", "#3380FF"
        )
        return if (gbsId != null) {
            val index = ((gbsId % colors.size) + colors.size) % colors.size
            colors[index.toInt()]
        } else {
            "#CCCCCC"
        }
    }

    /**
     * GBS 배치 정보를 저장합니다.
     */
    @Transactional
    fun saveGbsAssignment(villageId: Long, request: GbsAssignmentSaveRequest): GbsAssignmentSaveResponse {
        val village = getVillageById(villageId)
        val (totalMemberCount, assignmentCount) = processGbsAssignments(villageId, request)
        
        return GbsAssignmentSaveResponse(
            villageId = villageId,
            assignmentCount = assignmentCount,
            memberCount = totalMemberCount,
            message = "GBS 배치가 성공적으로 저장되었습니다."
        )
    }
    
    /**
     * GBS 배치 정보를 처리합니다.
     */
    private fun processGbsAssignments(villageId: Long, request: GbsAssignmentSaveRequest): Pair<Int, Int> {
        var totalMemberCount = 0
        val assignmentCount = request.assignments.size
        
        request.assignments.forEach { assignment ->
            val gbsGroup = validateAndGetGbsGroup(assignment.gbsId, villageId)
            val leader = getUserById(assignment.leaderId)
            
            processLeaderAssignment(assignment, gbsGroup, leader, request.startDate)
            totalMemberCount += processMemberAssignment(assignment, gbsGroup, request.startDate)
        }
        
        return Pair(totalMemberCount, assignmentCount)
    }
    
    /**
     * GBS 그룹을 검증하고 조회합니다.
     */
    private fun validateAndGetGbsGroup(gbsId: Long, villageId: Long): com.attendly.domain.entity.GbsGroup {
        val gbsGroup = gbsGroupRepository.findById(gbsId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.GBS_GROUP_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsId)
                ) 
            }
        
        if (gbsGroup.village.id != villageId) {
            throw AttendlyApiException(
                ErrorMessage.GBS_GROUP_NOT_IN_VILLAGE,
                "GBS ID: $gbsId, Village ID: $villageId"
            )
        }
        
        return gbsGroup
    }
    
    /**
     * 사용자 ID로 사용자 정보를 조회합니다.
     */
    private fun getUserById(userId: Long): User {
        return userRepository.findById(userId)
            .orElseThrow { 
                AttendlyApiException(
                    ErrorMessage.USER_NOT_FOUND, 
                    ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId)
                ) 
            }
    }
    
    /**
     * 리더 배치를 처리합니다.
     */
    private fun processLeaderAssignment(
        assignment: GbsAssignment,
        gbsGroup: com.attendly.domain.entity.GbsGroup,
        leader: User,
        startDate: LocalDate
    ) {
        val currentLeaderHistory = gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(assignment.gbsId, startDate)
        
        if (shouldUpdateLeaderHistory(currentLeaderHistory, assignment.leaderId)) {
            if (currentLeaderHistory != null) {
                terminateLeaderHistory(currentLeaderHistory, startDate)
            }
            createNewLeaderHistory(gbsGroup, leader, startDate)
        }
    }
    
    /**
     * 리더 히스토리를 업데이트해야 하는지 확인합니다.
     */
    private fun shouldUpdateLeaderHistory(currentLeaderHistory: GbsLeaderHistory?, newLeaderId: Long): Boolean {
        return currentLeaderHistory == null || currentLeaderHistory.leader.id != newLeaderId
    }
    
    /**
     * 기존 리더 히스토리를 종료합니다.
     */
    private fun terminateLeaderHistory(currentLeaderHistory: GbsLeaderHistory, startDate: LocalDate) {
        val updatedLeaderHistory = GbsLeaderHistory(
            id = currentLeaderHistory.id,
            gbsGroup = currentLeaderHistory.gbsGroup,
            leader = currentLeaderHistory.leader,
            startDate = currentLeaderHistory.startDate,
            endDate = startDate.minusDays(1)
        )
        gbsLeaderHistoryRepository.save(updatedLeaderHistory)
    }
    
    /**
     * 새로운 리더 히스토리를 생성합니다.
     */
    private fun createNewLeaderHistory(gbsGroup: com.attendly.domain.entity.GbsGroup, leader: User, startDate: LocalDate) {
        val newLeaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = startDate,
            endDate = null
        )
        gbsLeaderHistoryRepository.save(newLeaderHistory)
    }
    
    /**
     * 멤버 배치를 처리합니다.
     */
    private fun processMemberAssignment(
        assignment: GbsAssignment,
        gbsGroup: com.attendly.domain.entity.GbsGroup,
        startDate: LocalDate
    ): Int {
        val currentMemberHistories = gbsMemberHistoryRepository.findCurrentMembersByGbsId(assignment.gbsId, startDate)
        val currentMemberIds = currentMemberHistories.map { it.member.id!! }.toSet()
        val newMemberIds = assignment.memberIds.toSet()
        
        terminateRemovedMembers(currentMemberHistories, newMemberIds, startDate)
        addNewMembers(newMemberIds, currentMemberIds, gbsGroup, startDate)
        
        return 1 + assignment.memberIds.size // 리더 + 멤버 수
    }
    
    /**
     * 제거된 멤버들의 히스토리를 종료합니다.
     */
    private fun terminateRemovedMembers(
        currentMemberHistories: List<GbsMemberHistory>,
        newMemberIds: Set<Long>,
        startDate: LocalDate
    ) {
        val removedMemberHistories = currentMemberHistories.filter { it.member.id!! !in newMemberIds }
        removedMemberHistories.forEach { history ->
            val updatedHistory = GbsMemberHistory(
                id = history.id,
                gbsGroup = history.gbsGroup,
                member = history.member,
                startDate = history.startDate,
                endDate = startDate.minusDays(1)
            )
            gbsMemberHistoryRepository.save(updatedHistory)
        }
    }
    
    /**
     * 새로운 멤버들을 추가합니다.
     */
    private fun addNewMembers(
        newMemberIds: Set<Long>,
        currentMemberIds: Set<Long>,
        gbsGroup: com.attendly.domain.entity.GbsGroup,
        startDate: LocalDate
    ) {
        val addedMemberIds = newMemberIds - currentMemberIds
        addedMemberIds.forEach { memberId ->
            val member = getUserById(memberId)
            val newMemberHistory = GbsMemberHistory(
                gbsGroup = gbsGroup,
                member = member,
                startDate = startDate,
                endDate = null
            )
            gbsMemberHistoryRepository.save(newMemberHistory)
        }
    }

    /**
     * GBS 리더로 지정할 수 있는 사용자 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    fun getLeaderCandidates(villageId: Long): LeaderCandidateResponse {
        val village = getVillageById(villageId)
        val candidates = buildLeaderCandidateList(villageId)
        
        return LeaderCandidateResponse(
            villageId = villageId,
            villageName = village.name,
            candidates = candidates
        )
    }
    
    /**
     * 리더 후보 목록을 생성합니다.
     */
    private fun buildLeaderCandidateList(villageId: Long): List<LeaderCandidate> {
        val villageUsers = userRepository.findByFilters(
            UserFilterDto(
                villageId = villageId,
                roles = listOf(Role.LEADER)
            )
        )
        
        val candidates = villageUsers.map { user ->
            val previousGbsHistories = gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(user.id!!)
            val isCurrentlyLeader = gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(user.id) != null
            
            LeaderCandidate(
                id = user.id,
                name = user.name,
                email = user.email ?: "",
                isLeader = isCurrentlyLeader,
                previousGbsCount = previousGbsHistories.size
            )
        }
        
        return candidates.sortedWith(
            compareByDescending<LeaderCandidate> { it.isLeader }
                .thenByDescending { it.previousGbsCount }
        )
    }
} 