package com.church.attendly.service

import com.church.attendly.api.dto.AttendanceBatchRequest
import com.church.attendly.api.dto.AttendanceResponse
import com.church.attendly.api.dto.GbsAttendanceSummary
import com.church.attendly.api.dto.VillageAttendanceResponse
import com.church.attendly.domain.entity.Attendance
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.entity.WorshipStatus
import com.church.attendly.domain.repository.AttendanceRepository
import com.church.attendly.domain.repository.GbsGroupRepository
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.GbsMemberHistoryRepository
import com.church.attendly.domain.repository.LeaderDelegationRepository
import com.church.attendly.domain.repository.UserRepository
import com.church.attendly.domain.repository.VillageRepository
import com.church.attendly.exception.ResourceNotFoundException
import com.church.attendly.security.UserDetailsAdapter
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val gbsGroupRepository: GbsGroupRepository,
    private val userRepository: UserRepository,
    private val gbsLeaderHistoryRepository: GbsLeaderHistoryRepository,
    private val gbsMemberHistoryRepository: GbsMemberHistoryRepository,
    private val leaderDelegationRepository: LeaderDelegationRepository,
    private val villageRepository: VillageRepository
) {
    
    /**
     * 현재 인증된 사용자 조회
     */
    private fun getCurrentUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val userDetails = authentication.principal as UserDetailsAdapter
        return userDetails.getUser()
    }
    
    /**
     * 사용자가 해당 GBS에 대한 리더 권한이 있는지 확인
     */
    fun hasLeaderAccess(gbsId: Long, userId: Long): Boolean {
        // 1. 해당 GBS의 현재 리더인지 확인
        val isDirectLeader = gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) != null
        
        if (isDirectLeader) {
            return true
        }
        
        // 2. 위임된 권한이 있는지 확인
        val today = LocalDate.now()
        val delegations = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today)
        
        return delegations.isNotEmpty()
    }
    
    /**
     * 출석 일괄 등록
     */
    @Transactional
    fun createAttendances(request: AttendanceBatchRequest): List<AttendanceResponse> {
        val currentUser = getCurrentUser()
        val gbsGroup = gbsGroupRepository.findById(request.gbsId)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: ${request.gbsId}") }
        
        // 현재 사용자의 역할이 리더가 아니고, 리더 권한이 없으면 접근 거부
        if (currentUser.role != Role.ADMIN && 
            currentUser.role != Role.MINISTER && 
            currentUser.role != Role.VILLAGE_LEADER && 
            !hasLeaderAccess(request.gbsId, currentUser.id!!)) {
            throw AccessDeniedException("이 GBS에 대한 출석 입력 권한이 없습니다")
        }
        
        // 기존 출석 데이터 삭제 (동일 주차의 출석 데이터가 있으면 갱신)
        val existingAttendances = attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, request.weekStart)
        if (existingAttendances.isNotEmpty()) {
            attendanceRepository.deleteAll(existingAttendances)
        }
        
        // 새 출석 데이터 등록
        val attendances = request.attendances.map { item ->
            val member = userRepository.findById(item.memberId)
                .orElseThrow { ResourceNotFoundException("조원을 찾을 수 없습니다: ${item.memberId}") }
            
            Attendance(
                member = member,
                gbsGroup = gbsGroup,
                weekStart = request.weekStart,
                worship = item.worship,
                qtCount = item.qtCount,
                ministry = item.ministry,
                createdBy = currentUser
            )
        }
        
        val savedAttendances = attendanceRepository.saveAll(attendances)
        
        return savedAttendances.map { mapToAttendanceResponse(it) }
    }
    
    /**
     * GBS 출석 조회
     */
    @Transactional(readOnly = true)
    fun getAttendancesByGbs(gbsId: Long, weekStart: LocalDate): List<AttendanceResponse> {
        val gbsGroup = gbsGroupRepository.findById(gbsId)
            .orElseThrow { ResourceNotFoundException("GBS 그룹을 찾을 수 없습니다: $gbsId") }
        
        val attendances = attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart)
        
        return attendances.map { mapToAttendanceResponse(it) }
    }
    
    /**
     * 마을 출석 현황 조회
     */
    @Transactional(readOnly = true)
    fun getVillageAttendance(villageId: Long, weekStart: LocalDate): VillageAttendanceResponse {
        val village = villageRepository.findById(villageId)
            .orElseThrow { ResourceNotFoundException("마을을 찾을 수 없습니다: $villageId") }
        
        // 현재 활성화된 GBS 그룹 조회
        val today = LocalDate.now()
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, today)
        
        // 각 GBS별 출석 현황 집계
        val gbsAttendances = gbsGroups.map { gbsGroup ->
            val attendances = attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup.id!!, weekStart)
            
            // 현재 리더 정보 조회
            val leader = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsGroup.id)
                ?: throw EntityNotFoundException("현재 GBS의 리더를 찾을 수 없습니다: ${gbsGroup.id}")
            
            // 현재 GBS 멤버 수 조회
            val totalMembers = gbsMemberHistoryRepository.countActiveMembers(gbsGroup.id, today)
            
            // 출석한 멤버 수 계산
            val attendedMembers = attendances.count { it.worship == WorshipStatus.O }
            
            // 출석률 계산
            val attendanceRate = if (totalMembers > 0) {
                (attendedMembers.toDouble() / totalMembers) * 100
            } else {
                0.0
            }
            
            GbsAttendanceSummary(
                gbsId = gbsGroup.id,
                gbsName = gbsGroup.name,
                leaderName = leader.name,
                totalMembers = totalMembers.toInt(),
                attendedMembers = attendedMembers,
                attendanceRate = attendanceRate,
                memberAttendances = attendances.map { mapToAttendanceResponse(it) }
            )
        }
        
        return VillageAttendanceResponse(
            villageId = village.id!!,
            villageName = village.name,
            weekStart = weekStart,
            gbsAttendances = gbsAttendances
        )
    }
    
    /**
     * Attendance 엔티티를 AttendanceResponse DTO로 변환
     */
    private fun mapToAttendanceResponse(attendance: Attendance): AttendanceResponse {
        return AttendanceResponse(
            id = attendance.id!!,
            memberId = attendance.member.id!!,
            memberName = attendance.member.name,
            weekStart = attendance.weekStart,
            worship = attendance.worship,
            qtCount = attendance.qtCount,
            ministry = attendance.ministry
        )
    }
} 