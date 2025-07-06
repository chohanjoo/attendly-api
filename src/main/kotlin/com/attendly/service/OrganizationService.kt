package com.attendly.service

import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.VillageGbsInfoResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import com.attendly.domain.model.VillageGbsWithLeaderInfo
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class OrganizationService(
    private val departmentRepository: DepartmentRepository,
    private val villageRepository: VillageRepository,
    private val gbsGroupRepository: GbsGroupRepository,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository
) {
    /**
     * 더 간결한 코드를 위한 확장 함수
     */
    private fun ErrorMessage.withId(id: Long): AttendlyApiException {
        return AttendlyApiException(this, ErrorMessageUtils.withId(this, id))
    }
    
    private fun ErrorMessage.withIdAndDate(id: Long, date: LocalDate): AttendlyApiException {
        return AttendlyApiException(this, ErrorMessageUtils.withIdAndDate(this, id, date))
    }

    @Transactional(readOnly = true)
    fun getAllDepartments(): List<Department> {
        return departmentRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun getDepartmentById(id: Long): Department {
        return departmentRepository.findById(id)
            .orElseThrow { ErrorMessage.DEPARTMENT_NOT_FOUND.withId(id) }
    }

    @Transactional(readOnly = true)
    fun getVillagesByDepartment(departmentId: Long): List<Village> {
        val department = getDepartmentById(departmentId)
        return villageRepository.findByDepartment(department)
    }

    @Transactional(readOnly = true)
    fun getVillageById(id: Long): Village {
        return villageRepository.findById(id)
            .orElseThrow { ErrorMessage.VILLAGE_NOT_FOUND.withId(id) }
    }

    @Transactional(readOnly = true)
    fun getGbsGroupsByVillage(villageId: Long): List<GbsGroup> {
        val village = getVillageById(villageId)
        return gbsGroupRepository.findByVillage(village)
    }

    @Transactional(readOnly = true)
    fun getActiveGbsGroupsByVillage(villageId: Long, date: LocalDate = LocalDate.now()): List<GbsGroup> {
        return gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
    }

    @Transactional(readOnly = true)
    fun getGbsGroupById(id: Long): GbsGroup {
        return gbsGroupRepository.findById(id)
            .orElseThrow { ErrorMessage.GBS_GROUP_NOT_FOUND.withId(id) }
    }

    @Transactional(readOnly = true)
    fun getCurrentLeaderForGbs(gbsId: Long): String {
        val leader = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsId)
            ?: throw ErrorMessage.NO_ACTIVE_LEADER.withIdAndDate(gbsId, LocalDate.now())
        
        return leader.name
    }

    @Transactional(readOnly = true)
    fun getGbsWithLeader(gbsId: Long): Pair<GbsGroup, String?> {
        val gbsWithLeader = gbsGroupRepository.findWithCurrentLeader(gbsId)
            ?: throw ErrorMessage.GBS_GROUP_NOT_FOUND.withId(gbsId)
        
        return Pair(
            gbsWithLeader.gbsGroup,
            gbsWithLeader.leader?.name
        )
    }

    @Transactional(readOnly = true)
    fun getGbsMembers(gbsId: Long, date: LocalDate = LocalDate.now()): GbsMembersListResponse {
        val gbsGroup = getGbsGroupById(gbsId)
        
        // 해당 GBS의 현재 활성 멤버들 조회
        val condition = GbsMemberHistorySearchCondition(
            gbsId = gbsId,
            startDate = date,
            endDate = date
        )
        
        val members = gbsMemberHistoryRepository.findActiveMembers(condition)
        
        return GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = gbsGroup.name,
            memberCount = members.size,
            members = members.map { GbsMemberResponse.from(it) }
        )
    }
    
    /**
     * 마을장을 위한 마을내 모든 GBS 정보 조회
     * 각 GBS별 리더 및 조원 정보를 포함하여 반환
     * 
     * 최적화된 버전: N+1 문제 해결
     * - 기존: 2 + 2N개의 쿼리 (N은 GBS 개수)
     * - 개선: 3개의 쿼리 (Village 조회 + GBS&리더 정보 조회 + 모든 멤버 조회)
     */
    @Transactional(readOnly = true)
    fun getVillageGbsInfo(villageId: Long, date: LocalDate = LocalDate.now()): VillageGbsInfoResponse {
        val village = getVillageById(villageId)
        
        // 1. 마을의 모든 GBS와 리더 정보를 한 번에 조회
        val gbsWithLeaderInfoList = gbsGroupRepository.findVillageGbsWithLeaderInfo(villageId, date)
        
        if (gbsWithLeaderInfoList.isEmpty()) {
            return VillageGbsInfoResponse(
                villageId = villageId,
                villageName = village.name,
                gbsCount = 0,
                totalMemberCount = 0,
                gbsList = emptyList()
            )
        }
        
        // 2. 모든 GBS의 멤버들을 한 번에 조회
        val gbsIds = gbsWithLeaderInfoList.map { it.gbsId }
        val allMembers = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(gbsIds, date)
        
        // 3. GBS별로 멤버들을 그룹화
        val membersByGbsId = allMembers.groupBy { it.gbsGroup.id!! }
        
        // 4. 결과 생성
        val gbsInfoList = gbsWithLeaderInfoList.map { gbsInfo ->
            val members = membersByGbsId[gbsInfo.gbsId] ?: emptyList()
            
            VillageGbsInfoResponse.GbsInfo(
                gbsId = gbsInfo.gbsId,
                gbsName = gbsInfo.gbsName,
                leaderId = gbsInfo.leaderId,
                leaderName = gbsInfo.leaderName ?: "리더 없음",
                memberCount = members.size,
                members = members.map { GbsMemberResponse.from(it) }
            )
        }
        
        return VillageGbsInfoResponse(
            villageId = villageId,
            villageName = village.name,
            gbsCount = gbsInfoList.size,
            totalMemberCount = gbsInfoList.sumOf { it.memberCount },
            gbsList = gbsInfoList
        )
    }

    @Transactional(readOnly = true)
    fun getVillageWithActiveGbsGroups(villageId: Long, date: LocalDate = LocalDate.now()): Pair<Village, List<GbsGroup>> {
        val villageWithGbsGroups = villageRepository.findVillageWithActiveGbsGroups(villageId, date)
            ?: throw ErrorMessage.VILLAGE_NOT_FOUND.withId(villageId)

        return Pair(villageWithGbsGroups, villageWithGbsGroups.gbsGroups)
    }
} 