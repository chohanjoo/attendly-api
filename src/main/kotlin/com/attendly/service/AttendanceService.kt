package com.attendly.service

import com.attendly.api.dto.AttendanceBatchRequest
import com.attendly.api.dto.AttendanceResponse
import com.attendly.api.dto.GbsAttendanceSummary
import com.attendly.api.dto.VillageAttendanceResponse
import com.attendly.domain.entity.Attendance
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.domain.entity.WorshipStatus
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import com.attendly.security.UserDetailsAdapter
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(this.javaClass)!!

    /**
     * 현재 인증된 사용자 조회
     */
    private fun getCurrentUser(): User =
        (SecurityContextHolder.getContext().authentication.principal as UserDetailsAdapter).getUser()
    
    /**
     * 사용자가 해당 GBS에 대한 리더 권한이 있는지 확인
     */
    fun hasLeaderAccess(gbsId: Long, userId: Long): Boolean {
        // 1. 해당 GBS의 현재 리더인지 확인
        val isDirectLeader = gbsLeaderHistoryRepository
            .findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) != null
        
        if (isDirectLeader) return true
        
        // 2. 위임된 권한이 있는지 확인
        val today = LocalDate.now()
        return leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today).isNotEmpty()
    }
    
    /**
     * 사용자의 리더 접근 권한 확인
     */
    private fun validateLeaderAccess(currentUser: User, gbsId: Long) {
        val isAdmin = currentUser.role in listOf(Role.ADMIN, Role.MINISTER, Role.VILLAGE_LEADER)
        if (isAdmin) return

        val hasGbsAccess = hasLeaderAccess(gbsId, currentUser.id!!)
        if (!hasGbsAccess) {
            throw AttendlyApiException(ErrorMessage.ACCESS_DENIED_ATTENDANCE)
        }
    }
    
    /**
     * 출석 일괄 등록
     */
    @Transactional
    fun createAttendances(request: AttendanceBatchRequest): List<AttendanceResponse> {
        val currentUser = getCurrentUser()
        val gbsGroup = getGbsGroup(request.gbsId)
        
        validateWeekStartIsSunday(request.weekStart)
        validateLeaderAccess(currentUser, request.gbsId)
        deleteExistingAttendances(gbsGroup, request.weekStart)
        
        val attendances = createAttendanceEntities(request, gbsGroup, currentUser)
        return attendanceRepository.saveAll(attendances).map { it.toAttendanceResponse() }
    }
    
    /**
     * 주 시작일이 일요일인지 검증
     */
    private fun validateWeekStartIsSunday(weekStart: LocalDate) {
        if (weekStart.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            throw AttendlyApiException(
                ErrorMessage.INVALID_WEEK_START,
                "주 시작일은 일요일이어야 합니다. 입력된 날짜: ${weekStart}"
            )
        }
    }
    
    /**
     * GBS 그룹 조회
     */
    private fun getGbsGroup(gbsId: Long): GbsGroup {
        return gbsGroupRepository.findById(gbsId)
            .orElseThrow { 
                AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsId)) 
            }
    }
    
    /**
     * 기존 출석 데이터 삭제
     */
    private fun deleteExistingAttendances(gbsGroup: GbsGroup, weekStart: LocalDate) {
        attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart).let { 
            if (it.isNotEmpty()) attendanceRepository.deleteAll(it) 
        }
    }
    
    /**
     * 출석 엔티티 생성
     */
    private fun createAttendanceEntities(
        request: AttendanceBatchRequest, 
        gbsGroup: GbsGroup, 
        currentUser: User
    ): List<Attendance> {
        return request.attendances.map { item ->
            val member = getMember(item.memberId)
            validateMemberInGbs(member.id!!, request.gbsId)
            
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
    }
    
    /**
     * 조원 조회
     */
    private fun getMember(memberId: Long): User {
        return userRepository.findById(memberId)
            .orElseThrow { 
                AttendlyApiException(ErrorMessage.MEMBER_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.MEMBER_NOT_FOUND, memberId)) 
            }
    }
    
    /**
     * 조원이 GBS에 속하는지 확인
     */
    private fun validateMemberInGbs(memberId: Long, gbsId: Long) {
        val today = LocalDate.now()
        val isMemberOfGbs = gbsMemberHistoryRepository.findActiveMembers(
            GbsMemberHistorySearchCondition(
                gbsId = gbsId,
                startDate = today,
                endDate = today
            )
        ).any { it.member.id == memberId }
        
        if (!isMemberOfGbs) {
            throw AttendlyApiException(
                ErrorMessage.MEMBER_NOT_IN_GBS, 
                ErrorMessageUtils.withIds(ErrorMessage.MEMBER_NOT_IN_GBS, "memberId" to memberId, "gbsId" to gbsId)
            )
        }
    }
    
    /**
     * GBS 출석 조회
     */
    @Transactional(readOnly = true)
    fun getAttendancesByGbs(gbsId: Long, weekStart: LocalDate): List<AttendanceResponse> {
        val gbsGroup = gbsGroupRepository.findById(gbsId)
            .orElseThrow { 
                AttendlyApiException(ErrorMessage.GBS_GROUP_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsId)) 
            }
        
        return attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart)
            .map { it.toAttendanceResponse() }
    }
    
    /**
     * 마을 출석 현황 조회
     */
    @Transactional(readOnly = true)
    fun getVillageAttendance(villageId: Long, weekStart: LocalDate): VillageAttendanceResponse {
        val village = villageRepository.findById(villageId)
            .orElseThrow { 
                AttendlyApiException(ErrorMessage.VILLAGE_NOT_FOUND, ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId)) 
            }
        
        // 현재 날짜 대신 요청된 주간 시작일(weekStart)를 기준으로 활성화된 GBS 그룹 조회
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, weekStart)
        
        val gbsAttendances = gbsGroups.map { gbsGroup ->
            createGbsAttendanceSummary(gbsGroup, weekStart, weekStart)
        }
        
        return VillageAttendanceResponse(
            villageId = village.id!!,
            villageName = village.name,
            weekStart = weekStart,
            gbsAttendances = gbsAttendances
        )
    }
    
    /**
     * GBS 출석 요약 정보 생성
     */
    private fun createGbsAttendanceSummary(
        gbsGroup: GbsGroup,
        weekStart: LocalDate,
        referenceDate: LocalDate
    ): GbsAttendanceSummary {
        val gbsId = gbsGroup.id!!
        val attendances = attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, weekStart)

        log.debug("createGbsAttendanceSummary;gbsId: {}, weekStart: {}, attendances: {}", gbsId, weekStart, attendances)
        
        // 해당 날짜(referenceDate)에 활성화된 리더 조회
        val leader = gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsId, referenceDate)
            ?: throw AttendlyApiException(
                ErrorMessage.NO_ACTIVE_LEADER,
                ErrorMessageUtils.withIdAndDate(ErrorMessage.NO_ACTIVE_LEADER, gbsId, referenceDate)
            )

        log.debug("createGbsAttendanceSummary;leader: {}", leader)

        // 기준일(referenceDate)에 활성화된 멤버 수 조회
        val totalMembers = gbsMemberHistoryRepository.countActiveMembers(gbsId, referenceDate).toInt()

        log.debug("createGbsAttendanceSummary;totalMembers: {}", totalMembers)

        // 수정: 대예배 참석 여부와 관계없이 attendance 테이블에 기록된 모든 조원을 출석으로 인정
        val attendedMembers = attendances.size
        
        val attendanceRate = if (totalMembers > 0) {
            (attendedMembers.toDouble() / totalMembers) * 100
        } else 0.0
        
        return GbsAttendanceSummary(
            gbsId = gbsId,
            gbsName = gbsGroup.name,
            leaderName = leader.name,
            totalMembers = totalMembers,
            attendedMembers = attendedMembers,
            attendanceRate = attendanceRate,
            memberAttendances = attendances.map { it.toAttendanceResponse() }
        )
    }
    
    /**
     * Attendance 엔티티를 AttendanceResponse DTO로 변환
     */
    private fun Attendance.toAttendanceResponse(): AttendanceResponse =
        AttendanceResponse(
            id = id!!,
            memberId = member.id!!,
            memberName = member.name,
            weekStart = weekStart,
            worship = worship,
            qtCount = qtCount,
            ministry = ministry
        )
} 