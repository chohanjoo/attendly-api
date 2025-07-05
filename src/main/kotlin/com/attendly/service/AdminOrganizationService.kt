package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.*
import com.attendly.domain.repository.*
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository,
    private val villageLeaderRepository: VillageLeaderRepository
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
            .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId)) }

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
            throw AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId))
        }
        departmentRepository.deleteById(departmentId)
    }

    /**
     * 부서 조회
     */
    fun getDepartment(departmentId: Long): DepartmentResponse {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId)) }

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
            .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)) }

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
                .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, request.villageLeaderId)) }

            // 현재 마을장이 있는지 확인
            val existingVillageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(savedVillage.id!!)
            
            if (existingVillageLeader != null) {
                if (existingVillageLeader.user.id != request.villageLeaderId) {
                    // 기존 마을장과 새 마을장이 다르면 기존 마을장 종료 처리
                    val updatedVillageLeader = VillageLeader(
                        user = existingVillageLeader.user,
                        village = existingVillageLeader.village,
                        startDate = existingVillageLeader.startDate,
                        endDate = LocalDate.now(),
                        createdAt = existingVillageLeader.createdAt,
                        updatedAt = LocalDateTime.now()
                    )
                    villageLeaderRepository.save(updatedVillageLeader)
                    
                    // 새 마을장 등록
                    val villageLeader = VillageLeader(
                        user = leader,
                        village = savedVillage,
                        startDate = LocalDate.now()
                    )
                    villageLeaderRepository.save(villageLeader)
                }
            } else {
                // 마을장이 없으면 새로 등록
                val villageLeader = VillageLeader(
                    user = leader,
                    village = savedVillage,
                    startDate = LocalDate.now()
                )
                villageLeaderRepository.save(villageLeader)
            }

            villageLeaderId = leader.id
            villageLeaderName = leader.name
        } else {
            // 현재 마을장 정보 조회
            val existingVillageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(savedVillage.id!!)
            if (existingVillageLeader != null) {
                villageLeaderId = existingVillageLeader.user.id
                villageLeaderName = existingVillageLeader.user.name
            }
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
            .orElseThrow { AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)) }

        val department = if (request.departmentId != null && request.departmentId != village.department.id) {
            departmentRepository.findById(request.departmentId)
                .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)) }
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

        // 마을장 정보 조회 및 업데이트
        if (request.villageLeaderId != null) {
            val leader = userRepository.findById(request.villageLeaderId)
                .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, request.villageLeaderId)) }

            // 현재 마을장이 있는지 확인
            val existingVillageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId)
            
            if (existingVillageLeader != null) {
                if (existingVillageLeader.user.id != request.villageLeaderId) {
                    // 기존 마을장과 새 마을장이 다르면 기존 마을장 종료 처리
                    val updatedVillageLeader = VillageLeader(
                        user = existingVillageLeader.user,
                        village = existingVillageLeader.village,
                        startDate = existingVillageLeader.startDate,
                        endDate = LocalDate.now(),
                        createdAt = existingVillageLeader.createdAt,
                        updatedAt = LocalDateTime.now()
                    )
                    villageLeaderRepository.save(updatedVillageLeader)
                    
                    // 새 마을장 등록
                    val villageLeader = VillageLeader(
                        user = leader,
                        village = savedVillage,
                        startDate = LocalDate.now()
                    )
                    villageLeaderRepository.save(villageLeader)
                }
            } else {
                // 마을장이 없으면 새로 등록
                val villageLeader = VillageLeader(
                    user = leader,
                    village = savedVillage,
                    startDate = LocalDate.now()
                )
                villageLeaderRepository.save(villageLeader)
            }

            villageLeaderId = leader.id
            villageLeaderName = leader.name
        } else {
            // 현재 마을장 정보 조회
            val existingVillageLeader = villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId)
            if (existingVillageLeader != null) {
                villageLeaderId = existingVillageLeader.user.id
                villageLeaderName = existingVillageLeader.user.name
            }
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
     * GBS 그룹 생성
     */
    @Transactional
    fun createGbsGroup(request: GbsGroupCreateRequest): GbsGroupResponse {
        val village = villageRepository.findById(request.villageId)
            .orElseThrow { AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, request.villageId)) }

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
                .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, request.leaderId)) }

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
            .orElseThrow { AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsGroupId)) }

        val village = if (request.villageId != null && request.villageId != gbsGroup.village.id) {
            villageRepository.findById(request.villageId)
                .orElseThrow { AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, request.villageId)) }
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
            .orElseThrow { AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsGroupId)) }

        val leader = userRepository.findById(request.leaderId)
            .orElseThrow { AttendlyApiException(ErrorMessage.USER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, request.leaderId)) }

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
            .orElseThrow { AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsGroupId)) }

        val member = userRepository.findById(request.memberId)
            .orElseThrow { AttendlyApiException(ErrorMessage.MEMBER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.MEMBER_NOT_FOUND, request.memberId)) }

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
            .orElseThrow { AttendlyApiException(ErrorMessage.DEPARTMENT_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId)) }

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

    /**
     * 마을 목록 조회
     * 부서별, 이름으로 필터링이 가능합니다.
     */
    @Transactional(readOnly = true)
    fun getAllVillages(departmentId: Long?, name: String?, pageable: Pageable = PageRequest.of(0, 20)): PageResponse<VillageResponse> {
        val villages = villageRepository.findVillagesWithParams(departmentId, name, pageable)
        
        val items = villages.content.map { village ->
            // 마을장 정보 가져오기
            val villageLeader = village.villageLeader
            
            VillageResponse(
                id = village.id ?: 0L,
                name = village.name,
                departmentId = village.department.id ?: 0L,
                departmentName = village.department.name,
                villageLeaderId = villageLeader?.user?.id,
                villageLeaderName = villageLeader?.user?.name,
                createdAt = village.createdAt,
                updatedAt = village.updatedAt
            )
        }
        
        return PageResponse(
            items = items,
            totalCount = villages.totalElements,
            hasMore = villages.hasNext()
        )
    }

    /**
     * 모든 GBS 그룹 조회
     */
    @Transactional(readOnly = true)
    fun getAllGbsGroups(pageable: Pageable): PageResponse<AdminGbsGroupListResponse> {
        val gbsGroups = gbsGroupRepository.findAllGbsGroupsWithCompleteDetails(pageable)
        
        val items = gbsGroups.content.map { it.toResponse() }
        
        return PageResponse(
            items = items,
            totalCount = gbsGroups.totalElements,
            hasMore = gbsGroups.hasNext()
        )
    }
} 