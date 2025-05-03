package com.church.attendly.service

import com.church.attendly.api.dto.GbsMemberResponse
import com.church.attendly.api.dto.GbsMembersListResponse
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.Village
import com.church.attendly.domain.model.GbsMemberHistorySearchCondition
import com.church.attendly.domain.repository.DepartmentRepository
import com.church.attendly.domain.repository.GbsGroupRepository
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.GbsMemberHistoryRepository
import com.church.attendly.domain.repository.VillageRepository
import com.church.attendly.exception.ResourceNotFoundException
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

    @Transactional(readOnly = true)
    @Cacheable("departments")
    fun getAllDepartments(): List<Department> {
        return departmentRepository.findAll()
    }

    @Transactional(readOnly = true)
    @Cacheable("department")
    fun getDepartmentById(id: Long): Department {
        return departmentRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("부서를 찾을 수 없습니다: $id") }
    }

    @Transactional(readOnly = true)
    @Cacheable("villagesByDepartment")
    fun getVillagesByDepartment(departmentId: Long): List<Village> {
        val department = getDepartmentById(departmentId)
        return villageRepository.findByDepartment(department)
    }

    @Transactional(readOnly = true)
    @Cacheable("village")
    fun getVillageById(id: Long): Village {
        return villageRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("마을을 찾을 수 없습니다: $id") }
    }

    @Transactional(readOnly = true)
    @Cacheable("gbsGroups")
    fun getGbsGroupsByVillage(villageId: Long): List<GbsGroup> {
        val village = getVillageById(villageId)
        return gbsGroupRepository.findByVillage(village)
    }

    @Transactional(readOnly = true)
    @Cacheable("activeGbsGroups")
    fun getActiveGbsGroupsByVillage(villageId: Long, date: LocalDate = LocalDate.now()): List<GbsGroup> {
        return gbsGroupRepository.findActiveGroupsByVillageId(villageId, date)
    }

    @Transactional(readOnly = true)
    @Cacheable("gbsGroup")
    fun getGbsGroupById(id: Long): GbsGroup {
        return gbsGroupRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: $id") }
    }

    @Transactional(readOnly = true)
    @Cacheable("gbsLeader")
    fun getCurrentLeaderForGbs(gbsId: Long): String {
        val leader = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsId)
            ?: throw ResourceNotFoundException("현재 GBS의 리더를 찾을 수 없습니다: $gbsId")
        
        return leader.name
    }

    @Transactional(readOnly = true)
    @Cacheable("gbsMembers")
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
} 