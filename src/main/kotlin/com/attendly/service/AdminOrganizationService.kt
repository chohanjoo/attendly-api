package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.*
import com.attendly.domain.repository.*
import com.attendly.exception.ResourceNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AdminOrganizationService(
    private val departmentRepository: DepartmentRepository,
    private val villageRepository: VillageRepository,
    private val gbsGroupRepository: GbsGroupRepository,
    private val userRepository: UserRepository,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository
) {

    /**
     * 부서 생성
     */
    @Transactional
    fun createDepartment(request: DepartmentCreateRequest): DepartmentResponse {
        val department = Department(
            name = request.name
        )

        val savedDepartment = departmentRepository.save(department)

        return DepartmentResponse(
            id = savedDepartment.id ?: 0L,
            name = savedDepartment.name,
            createdAt = savedDepartment.createdAt,
            updatedAt = savedDepartment.updatedAt
        )
    }

    /**
     * 부서 수정
     */
    @Transactional
    fun updateDepartment(departmentId: Long, request: DepartmentUpdateRequest): DepartmentResponse {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: ID $departmentId") }

        val updatedDepartment = Department(
            id = department.id,
            name = request.name,
            createdAt = department.createdAt
        )

        val savedDepartment = departmentRepository.save(updatedDepartment)

        return DepartmentResponse(
            id = savedDepartment.id ?: 0L,
            name = savedDepartment.name,
            createdAt = savedDepartment.createdAt,
            updatedAt = savedDepartment.updatedAt
        )
    }

    /**
     * 부서 삭제
     */
    @Transactional
    fun deleteDepartment(departmentId: Long) {
        if (!departmentRepository.existsById(departmentId)) {
            throw ResourceNotFoundException("부서를 찾을 수 없습니다: ID $departmentId")
        }
        departmentRepository.deleteById(departmentId)
    }

    /**
     * 부서 조회
     */
    fun getDepartment(departmentId: Long): DepartmentResponse {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: ID $departmentId") }

        return DepartmentResponse(
            id = department.id ?: 0L,
            name = department.name,
            createdAt = department.createdAt,
            updatedAt = department.updatedAt
        )
    }

    /**
     * 모든 부서 조회
     */
    fun getAllDepartments(): List<DepartmentResponse> {
        return departmentRepository.findAll().map { department ->
            DepartmentResponse(
                id = department.id ?: 0L,
                name = department.name,
                createdAt = department.createdAt,
                updatedAt = department.updatedAt
            )
        }
    }

    /**
     * 마을 생성
     */
    @Transactional
    fun createVillage(request: VillageCreateRequest): VillageResponse {
        val department = departmentRepository.findById(request.departmentId)
            .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: ID ${request.departmentId}") }

        val village = Village(
            name = request.name,
            department = department
        )

        val savedVillage = villageRepository.save(village)

        // 마을장 지정이 있는 경우
        var villageLeaderId: Long? = null
        var villageLeaderName: String? = null

        if (request.villageLeaderId != null) {
            val leader = userRepository.findById(request.villageLeaderId)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ID ${request.villageLeaderId}") }

            val villageLeader = VillageLeader(
                village = savedVillage,
                user = leader,
                startDate = LocalDate.now()
            )

            // TODO: 마을장 정보 조회 및 업데이트 로직 추가

            villageLeaderId = leader.id
            villageLeaderName = leader.name
        }

        return VillageResponse(
            id = savedVillage.id ?: 0L,
            name = savedVillage.name,
            departmentId = savedVillage.department.id ?: 0L,
            departmentName = savedVillage.department.name,
            villageLeaderId = villageLeaderId,
            villageLeaderName = villageLeaderName,
            createdAt = savedVillage.createdAt,
            updatedAt = savedVillage.updatedAt
        )
    }

    /**
     * 마을 수정
     */
    @Transactional
    fun updateVillage(villageId: Long, request: VillageUpdateRequest): VillageResponse {
        val village = villageRepository.findById(villageId)
            .orElseThrow { ResourceNotFoundException("마을을 찾을 수 없습니다: ID $villageId") }

        val department = if (request.departmentId != null && request.departmentId != village.department.id) {
            departmentRepository.findById(request.departmentId)
                .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: ID ${request.departmentId}") }
        } else {
            village.department
        }

        val updatedVillage = Village(
            id = village.id,
            name = request.name,
            department = department,
            createdAt = village.createdAt
        )

        val savedVillage = villageRepository.save(updatedVillage)

        // 마을장 정보 준비
        var villageLeaderId: Long? = null
        var villageLeaderName: String? = null

        // TODO: 마을장 정보 조회 및 업데이트 로직 추가

        return VillageResponse(
            id = savedVillage.id ?: 0L,
            name = savedVillage.name,
            departmentId = savedVillage.department.id ?: 0L,
            departmentName = savedVillage.department.name,
            villageLeaderId = villageLeaderId,
            villageLeaderName = villageLeaderName,
            createdAt = savedVillage.createdAt,
            updatedAt = savedVillage.updatedAt
        )
    }

    /**
     * GBS 그룹 생성
     */
    @Transactional
    fun createGbsGroup(request: GbsGroupCreateRequest): GbsGroupResponse {
        val village = villageRepository.findById(request.villageId)
            .orElseThrow { ResourceNotFoundException("마을을 찾을 수 없습니다: ID ${request.villageId}") }

        val gbsGroup = GbsGroup(
            name = request.name,
            village = village,
            termStartDate = request.termStartDate,
            termEndDate = request.termEndDate
        )

        val savedGbsGroup = gbsGroupRepository.save(gbsGroup)

        // 리더 지정이 있는 경우
        var leaderName: String? = null
        var leaderId: Long? = null

        if (request.leaderId != null) {
            assignLeaderToGbs(savedGbsGroup.id ?: 0L, GbsLeaderAssignRequest(
                leaderId = request.leaderId,
                startDate = request.termStartDate
            ))

            val leader = userRepository.findById(request.leaderId)
                .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ID ${request.leaderId}") }

            leaderName = leader.name
            leaderId = leader.id
        }

        return GbsGroupResponse(
            id = savedGbsGroup.id ?: 0L,
            name = savedGbsGroup.name,
            villageId = savedGbsGroup.village.id ?: 0L,
            villageName = savedGbsGroup.village.name,
            termStartDate = savedGbsGroup.termStartDate,
            termEndDate = savedGbsGroup.termEndDate,
            leaderName = leaderName,
            leaderId = leaderId,
            createdAt = savedGbsGroup.createdAt,
            updatedAt = savedGbsGroup.updatedAt
        )
    }

    /**
     * GBS 그룹 수정
     */
    @Transactional
    fun updateGbsGroup(gbsGroupId: Long, request: GbsGroupUpdateRequest): GbsGroupResponse {
        val gbsGroup = gbsGroupRepository.findById(gbsGroupId)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: ID $gbsGroupId") }

        val village = if (request.villageId != null && request.villageId != gbsGroup.village.id) {
            villageRepository.findById(request.villageId)
                .orElseThrow { ResourceNotFoundException("마을을 찾을 수 없습니다: ID ${request.villageId}") }
        } else {
            gbsGroup.village
        }

        val updatedGbsGroup = GbsGroup(
            id = gbsGroup.id,
            name = request.name,
            village = village,
            termStartDate = request.termStartDate ?: gbsGroup.termStartDate,
            termEndDate = request.termEndDate ?: gbsGroup.termEndDate,
            createdAt = gbsGroup.createdAt
        )

        val savedGbsGroup = gbsGroupRepository.save(updatedGbsGroup)

        // 현재 리더 정보 조회
        val currentLeader = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsGroupId)
        val leaderName = currentLeader?.name
        val leaderId = currentLeader?.id

        return GbsGroupResponse(
            id = savedGbsGroup.id ?: 0L,
            name = savedGbsGroup.name,
            villageId = savedGbsGroup.village.id ?: 0L,
            villageName = savedGbsGroup.village.name,
            termStartDate = savedGbsGroup.termStartDate,
            termEndDate = savedGbsGroup.termEndDate,
            leaderName = leaderName,
            leaderId = leaderId,
            createdAt = savedGbsGroup.createdAt,
            updatedAt = savedGbsGroup.updatedAt
        )
    }

    /**
     * GBS 그룹에 리더 할당
     */
    @Transactional
    fun assignLeaderToGbs(gbsGroupId: Long, request: GbsLeaderAssignRequest) {
        val gbsGroup = gbsGroupRepository.findById(gbsGroupId)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: ID $gbsGroupId") }

        val leader = userRepository.findById(request.leaderId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ID ${request.leaderId}") }

        // 현재 리더가 있다면 종료 처리
        val currentLeaderHistory = gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsGroupId, request.leaderId)
        if (currentLeaderHistory != null && currentLeaderHistory.endDate == null) {
            val endDate = request.startDate.minusDays(1)
            
            val updatedHistory = GbsLeaderHistory(
                id = currentLeaderHistory.id,
                gbsGroup = currentLeaderHistory.gbsGroup,
                leader = currentLeaderHistory.leader,
                startDate = currentLeaderHistory.startDate,
                endDate = endDate,
                createdAt = currentLeaderHistory.createdAt
            )
            
            gbsLeaderHistoryRepository.save(updatedHistory)
        }

        // 새 리더 이력 생성
        val leaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = request.startDate,
            endDate = request.endDate
        )

        gbsLeaderHistoryRepository.save(leaderHistory)
    }

    /**
     * GBS 그룹에 조원 할당
     */
    @Transactional
    fun assignMemberToGbs(gbsGroupId: Long, request: GbsMemberAssignRequest) {
        val gbsGroup = gbsGroupRepository.findById(gbsGroupId)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: ID $gbsGroupId") }

        val member = userRepository.findById(request.memberId)
            .orElseThrow { ResourceNotFoundException("사용자를 찾을 수 없습니다: ID ${request.memberId}") }

        // 현재 다른 GBS 그룹에 속해 있는지 확인
        val currentMemberHistory = gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(request.memberId)
        if (currentMemberHistory != null && currentMemberHistory.endDate == null) {
            val endDate = request.startDate.minusDays(1)
            
            val updatedHistory = GbsMemberHistory(
                id = currentMemberHistory.id,
                gbsGroup = currentMemberHistory.gbsGroup,
                member = currentMemberHistory.member,
                startDate = currentMemberHistory.startDate,
                endDate = endDate,
                createdAt = currentMemberHistory.createdAt
            )
            
            gbsMemberHistoryRepository.save(updatedHistory)
        }

        // 새 조원 이력 생성
        val memberHistory = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = request.startDate,
            endDate = request.endDate
        )

        gbsMemberHistoryRepository.save(memberHistory)
    }

    /**
     * GBS 6개월주기 재편성 실행
     */
    @Transactional
    fun executeGbsReorganization(request: GbsReorganizationRequest): GbsReorganizationResponse {
        val department = departmentRepository.findById(request.departmentId)
            .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: ID ${request.departmentId}") }

        // 부서 내 모든 GBS 그룹 조회
        // TODO: 부서 내 모든 GBS 그룹 조회 로직 구현
        val affectedGbsCount = 0
        val affectedMemberCount = 0
        val affectedLeaderCount = 0

        // TODO: 실제 재편성 로직 구현
        
        return GbsReorganizationResponse(
            departmentId = department.id ?: 0L,
            departmentName = department.name,
            startDate = request.startDate,
            endDate = request.endDate,
            affectedGbsCount = affectedGbsCount,
            affectedMemberCount = affectedMemberCount,
            affectedLeaderCount = affectedLeaderCount,
            completedAt = LocalDateTime.now()
        )
    }
} 