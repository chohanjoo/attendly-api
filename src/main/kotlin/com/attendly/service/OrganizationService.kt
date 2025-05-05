package com.attendly.service

import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
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
    fun getAllDepartments(): List<Department> {
        return departmentRepository.findAll()
    }

    @Transactional(readOnly = true)
    fun getDepartmentById(id: Long): Department {
        return departmentRepository.findById(id)
            .orElseThrow { AttendlyApiException(ErrorCode.RESOURCE_NOT_FOUND, "부서를 찾을 수 없습니다: $id") }
    }

    @Transactional(readOnly = true)
    fun getVillagesByDepartment(departmentId: Long): List<Village> {
        val department = getDepartmentById(departmentId)
        return villageRepository.findByDepartment(department)
    }

    @Transactional(readOnly = true)
    fun getVillageById(id: Long): Village {
        return villageRepository.findById(id)
            .orElseThrow { AttendlyApiException(ErrorCode.RESOURCE_NOT_FOUND, "마을을 찾을 수 없습니다: $id") }
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
            .orElseThrow { AttendlyApiException(ErrorCode.RESOURCE_NOT_FOUND, "GBS 그룹을 찾을 수 없습니다: $id") }
    }

    @Transactional(readOnly = true)
    fun getCurrentLeaderForGbs(gbsId: Long): String {
        val leader = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsId)
            ?: throw AttendlyApiException(ErrorCode.RESOURCE_NOT_FOUND, "현재 GBS의 리더를 찾을 수 없습니다: $gbsId")
        
        return leader.name
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
} 