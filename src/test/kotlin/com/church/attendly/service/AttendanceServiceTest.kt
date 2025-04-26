package com.church.attendly.service

import com.church.attendly.api.dto.AttendanceBatchRequest
import com.church.attendly.api.dto.AttendanceItemRequest
import com.church.attendly.domain.entity.*
import com.church.attendly.domain.repository.*
import com.church.attendly.exception.ResourceNotFoundException
import com.church.attendly.security.UserDetailsAdapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class AttendanceServiceTest {

    @Mock
    private lateinit var attendanceRepository: AttendanceRepository

    @Mock
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @Mock
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @Mock
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository

    @Mock
    private lateinit var villageRepository: VillageRepository

    @Mock
    private lateinit var securityContext: SecurityContext

    @Mock
    private lateinit var authentication: Authentication

    @InjectMocks
    private lateinit var attendanceService: AttendanceService

    private lateinit var currentUser: User
    private lateinit var gbsGroup: GbsGroup
    private lateinit var village: Village
    private lateinit var department: Department

    @BeforeEach
    fun setUp() {
        // 부서 생성
        department = Department(
            id = 1L,
            name = "테스트 부서"
        )
        
        // 마을 생성
        village = Village(
            id = 1L,
            name = "테스트 마을",
            department = department
        )
        
        // GBS 그룹 생성
        val today = LocalDate.now()
        gbsGroup = GbsGroup(
            id = 1L,
            name = "테스트 GBS",
            village = village,
            termStartDate = today.minusMonths(6),
            termEndDate = today.plusMonths(6)
        )
        
        // 현재 사용자 설정
        currentUser = User(
            id = 1L,
            email = "test@example.com",
            password = "password",
            name = "테스트 사용자",
            role = Role.LEADER,
            department = department
        )
    }

    @Test
    @DisplayName("리더 권한 확인 - 직접 리더인 경우")
    fun testHasLeaderAccess_DirectLeader() {
        // given
        val gbsId = 1L
        val userId = 1L
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            leader = currentUser,
            gbsGroup = gbsGroup,
            startDate = LocalDate.now().minusDays(30)
        )

        `when`(gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)).thenReturn(leaderHistory)

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertTrue(result)
        verify(gbsLeaderHistoryRepository).findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)
    }

    @Test
    @DisplayName("리더 권한 확인 - 권한 위임된 경우")
    fun testHasLeaderAccess_DelegatedLeader() {
        // given
        val gbsId = 1L
        val userId = 1L
        val today = LocalDate.now()
        
        // 직접 리더가 아님
        `when`(gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)).thenReturn(null)
        
        // 위임된 권한이 있음
        val delegator = User(
            id = 2L, 
            email = "leader@example.com", 
            password = "pwd", 
            name = "리더", 
            role = Role.LEADER,
            department = department
        )
        
        val delegations = listOf(
            LeaderDelegation(
                id = 1L,
                delegator = delegator,
                delegatee = currentUser, 
                gbsGroup = gbsGroup,
                startDate = today.minusDays(7),
                endDate = today.plusDays(7)
            )
        )
        
        `when`(leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today)).thenReturn(delegations)

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertTrue(result)
        verify(gbsLeaderHistoryRepository).findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)
        verify(leaderDelegationRepository).findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today)
    }

    @Test
    @DisplayName("리더 권한 확인 - 권한 없는 경우")
    fun testHasLeaderAccess_NoAccess() {
        // given
        val gbsId = 1L
        val userId = 1L
        val today = LocalDate.now()
        
        // 직접 리더가 아님
        `when`(gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)).thenReturn(null)
        
        // 위임된 권한도 없음
        `when`(leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today)).thenReturn(emptyList())

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertFalse(result)
        verify(gbsLeaderHistoryRepository).findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId)
        verify(leaderDelegationRepository).findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today)
    }

    @Test
    @DisplayName("출석 일괄 등록 - 권한이 있는 경우")
    fun testCreateAttendances_WithValidAccess() {
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(currentUser)
        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.principal).thenReturn(userDetails)
        SecurityContextHolder.setContext(securityContext)
        
        // given
        val weekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        val member1 = User(
            id = 2L, 
            email = "member1@example.com", 
            password = "pwd", 
            name = "조원1", 
            role = Role.MEMBER,
            department = department
        )
        
        val member2 = User(
            id = 3L, 
            email = "member2@example.com", 
            password = "pwd", 
            name = "조원2", 
            role = Role.MEMBER,
            department = department
        )
        
        val batchRequest = AttendanceBatchRequest(
            gbsId = 1L,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceItemRequest(memberId = 2L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A),
                AttendanceItemRequest(memberId = 3L, worship = WorshipStatus.X, qtCount = 3, ministry = MinistryStatus.B)
            )
        )
        
        // GBS 그룹 조회
        `when`(gbsGroupRepository.findById(1L)).thenReturn(Optional.of(gbsGroup))
        
        // 리더 권한 확인
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            leader = currentUser,
            gbsGroup = gbsGroup,
            startDate = LocalDate.now().minusDays(30)
        )
        `when`(gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L)).thenReturn(leaderHistory)
        
        // 기존 출석 데이터 조회
        `when`(attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart)).thenReturn(emptyList())
        
        // 조원 정보 조회
        `when`(userRepository.findById(2L)).thenReturn(Optional.of(member1))
        `when`(userRepository.findById(3L)).thenReturn(Optional.of(member2))
        
        // 출석 정보 저장
        val savedAttendance1 = Attendance(
            id = 1L,
            member = member1,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = currentUser
        )
        
        val savedAttendance2 = Attendance(
            id = 2L,
            member = member2,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.X,
            qtCount = 3,
            ministry = MinistryStatus.B,
            createdBy = currentUser
        )
        
        `when`(attendanceRepository.saveAll(anyList())).thenReturn(listOf(savedAttendance1, savedAttendance2))
        
        // when
        val result = attendanceService.createAttendances(batchRequest)
        
        // then
        assertEquals(2L, result.size.toLong())
        assertEquals(2L, result[0].memberId)
        assertEquals("조원1", result[0].memberName)
        assertEquals(WorshipStatus.O, result[0].worship)
        
        verify(gbsGroupRepository).findById(1L)
        verify(attendanceRepository).findByGbsGroupAndWeekStart(gbsGroup, weekStart)
        verify(userRepository).findById(2L)
        verify(userRepository).findById(3L)
        verify(attendanceRepository).saveAll(anyList())
    }
    
    @Test
    @DisplayName("출석 일괄 등록 - 권한이 없는 경우 에러 발생")
    fun testCreateAttendances_WithoutAccess() {
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(currentUser)
        `when`(securityContext.authentication).thenReturn(authentication)
        `when`(authentication.principal).thenReturn(userDetails)
        SecurityContextHolder.setContext(securityContext)
        
        // given
        val weekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        
        val batchRequest = AttendanceBatchRequest(
            gbsId = 1L,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceItemRequest(memberId = 2L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A)
            )
        )
        
        // GBS 그룹 조회
        `when`(gbsGroupRepository.findById(1L)).thenReturn(Optional.of(gbsGroup))
        
        // 리더 권한이 없음
        `when`(gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L)).thenReturn(null)
        `when`(leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(1L, 1L, LocalDate.now())).thenReturn(emptyList())
        
        // when, then
        org.junit.jupiter.api.assertThrows<AccessDeniedException> {
            attendanceService.createAttendances(batchRequest)
        }
        
        verify(gbsGroupRepository).findById(1L)
        verify(gbsLeaderHistoryRepository).findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L)
        verify(leaderDelegationRepository).findActiveDelegationsByDelegateeAndGbs(1L, 1L, LocalDate.now())
    }
    
    @Test
    @DisplayName("GBS 출석 조회")
    fun testGetAttendancesByGbs() {
        // given
        val gbsId = 1L
        val weekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        val member = User(
            id = 2L, 
            email = "member@example.com", 
            password = "pwd", 
            name = "조원", 
            role = Role.MEMBER,
            department = department
        )
        
        val attendance = Attendance(
            id = 1L,
            member = member,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = currentUser
        )
        
        `when`(gbsGroupRepository.findById(gbsId)).thenReturn(Optional.of(gbsGroup))
        `when`(attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart)).thenReturn(listOf(attendance))
        
        // when
        val result = attendanceService.getAttendancesByGbs(gbsId, weekStart)
        
        // then
        assertEquals(1L, result.size.toLong())
        assertEquals(2L, result[0].memberId)
        assertEquals("조원", result[0].memberName)
        assertEquals(WorshipStatus.O, result[0].worship)
        
        verify(gbsGroupRepository).findById(gbsId)
        verify(attendanceRepository).findByGbsGroupAndWeekStart(gbsGroup, weekStart)
    }
    
    @Test
    @DisplayName("마을 출석 현황 조회")
    fun testGetVillageAttendance() {
        // given
        val villageId = 1L
        val weekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        val today = LocalDate.now()
        
        val leader = User(
            id = 4L, 
            email = "leader@example.com", 
            password = "pwd", 
            name = "리더", 
            role = Role.LEADER,
            department = department
        )
        
        val member = User(
            id = 2L, 
            email = "member@example.com", 
            password = "pwd", 
            name = "조원", 
            role = Role.MEMBER,
            department = department
        )
        
        val attendance = Attendance(
            id = 1L,
            member = member,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = currentUser
        )
        
        `when`(villageRepository.findById(villageId)).thenReturn(Optional.of(village))
        `when`(gbsGroupRepository.findActiveGroupsByVillageId(villageId, today)).thenReturn(listOf(gbsGroup))
        `when`(attendanceRepository.findDetailsByGbsIdAndWeek(1L, weekStart)).thenReturn(listOf(attendance))
        `when`(gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L)).thenReturn(leader)
        `when`(gbsMemberHistoryRepository.countActiveMembers(1L, today)).thenReturn(10L)
        
        // when
        val result = attendanceService.getVillageAttendance(villageId, weekStart)
        
        // then
        assertEquals(villageId, result.villageId)
        assertEquals("테스트 마을", result.villageName)
        assertEquals(1L, result.gbsAttendances.size.toLong())
        assertEquals(1L, result.gbsAttendances[0].gbsId)
        assertEquals("테스트 GBS", result.gbsAttendances[0].gbsName)
        assertEquals("리더", result.gbsAttendances[0].leaderName)
        assertEquals(10L, result.gbsAttendances[0].totalMembers.toLong())
        assertEquals<Long>(1L, result.gbsAttendances[0].attendedMembers.toLong())
        assertEquals<Double>(10.0, result.gbsAttendances[0].attendanceRate)
        
        verify(villageRepository).findById(villageId)
        verify(gbsGroupRepository).findActiveGroupsByVillageId(villageId, today)
        verify(attendanceRepository).findDetailsByGbsIdAndWeek(1L, weekStart)
        verify(gbsLeaderHistoryRepository).findCurrentLeaderByGbsId(1L)
        verify(gbsMemberHistoryRepository).countActiveMembers(1L, today)
    }
} 