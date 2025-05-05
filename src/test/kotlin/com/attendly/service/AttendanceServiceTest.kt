package com.attendly.service

import com.attendly.api.dto.AttendanceBatchRequest
import com.attendly.api.dto.AttendanceItemRequest
import com.attendly.domain.entity.*
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import com.attendly.domain.repository.*
import com.attendly.exception.ResourceNotFoundException
import com.attendly.security.UserDetailsAdapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows
import io.mockk.*
import io.mockk.junit5.MockKExtension
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.InjectMockKs

@ExtendWith(MockKExtension::class)
class AttendanceServiceTest {

    @MockK
    private lateinit var attendanceRepository: AttendanceRepository

    @MockK
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @MockK
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @MockK
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository

    @MockK
    private lateinit var villageRepository: VillageRepository

    @MockK
    private lateinit var securityContext: SecurityContext

    @MockK
    private lateinit var authentication: Authentication

    @InjectMockKs
    private lateinit var attendanceService: AttendanceService

    private lateinit var currentUser: User
    private lateinit var gbsGroup: GbsGroup
    private lateinit var village: Village
    private lateinit var department: Department
    private val today = LocalDate.now()
    private val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
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
            startDate = today.minusDays(30)
        )

        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) } returns leaderHistory

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertTrue(result)
        verify { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) }
        confirmVerified(leaderDelegationRepository)
    }

    @Test
    @DisplayName("리더 권한 확인 - 권한 위임된 경우")
    fun testHasLeaderAccess_DelegatedLeader() {
        // given
        val gbsId = 1L
        val userId = 1L
        
        // 직접 리더가 아님
        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) } returns null
        
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
        
        every { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today) } returns delegations

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertTrue(result)
        verify { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) }
        verify { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today) }
    }

    @Test
    @DisplayName("리더 권한 확인 - 권한 없는 경우")
    fun testHasLeaderAccess_NoAccess() {
        // given
        val gbsId = 1L
        val userId = 1L
        
        // 직접 리더가 아님
        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) } returns null
        
        // 위임된 권한도 없음
        every { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today) } returns emptyList()

        // when
        val result = attendanceService.hasLeaderAccess(gbsId, userId)

        // then
        assertFalse(result)
        verify { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsId, userId) }
        verify { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(userId, gbsId, today) }
    }

    @Test
    @DisplayName("리더 권한 검증 - 관리자 권한일 경우 항상 통과")
    fun testValidateLeaderAccess_Admin() {
        // given
        val adminUser = User(
            id = 5L,
            email = "admin@example.com",
            password = "password",
            name = "관리자",
            role = Role.ADMIN,
            department = department
        )
        
        val gbsId = 1L
        
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(adminUser)
        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns userDetails
        SecurityContextHolder.setContext(securityContext)
        
        val batchRequest = AttendanceBatchRequest(
            gbsId = gbsId,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceItemRequest(memberId = 2L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A)
            )
        )
        
        every { gbsGroupRepository.findById(gbsId) } returns Optional.of(gbsGroup)
        every { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) } returns emptyList()
        
        val member = User(
            id = 2L, 
            email = "member@example.com", 
            password = "pwd", 
            name = "조원", 
            role = Role.MEMBER,
            department = department
        )
        
        every { userRepository.findById(2L) } returns Optional.of(member)
        
        // GBS 멤버십 확인
        val activeMember = GbsMemberHistory(
            id = 1L,
            member = member,
            gbsGroup = gbsGroup,
            startDate = today.minusMonths(1),
            endDate = null
        )
        
        every { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        } returns listOf(activeMember)
        
        val savedAttendance = Attendance(
            id = 1L,
            member = member,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = adminUser
        )
        
        every { attendanceRepository.saveAll(any<List<Attendance>>()) } returns listOf(savedAttendance)
        
        // when - then
        // validateLeaderAccess는 private 메소드이므로 createAttendances를 통해 간접적으로 테스트
        // 관리자 권한이므로 예외 없이 정상 실행됨
        val result = attendanceService.createAttendances(batchRequest)
        
        assertEquals(1, result.size)
        assertEquals(2L, result[0].memberId)
        
        verify { userRepository.findById(2L) }
        verify { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        }
    }

    @Test
    @DisplayName("출석 일괄 등록 - 권한이 있는 경우")
    fun testCreateAttendances_WithValidAccess() {
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(currentUser)
        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns userDetails
        SecurityContextHolder.setContext(securityContext)
        
        // given
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
        every { gbsGroupRepository.findById(1L) } returns Optional.of(gbsGroup)
        
        // 리더 권한 확인
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            leader = currentUser,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(30)
        )
        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L) } returns leaderHistory
        
        // 기존 출석 데이터 조회
        every { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) } returns emptyList()
        
        // 조원 정보 조회
        every { userRepository.findById(2L) } returns Optional.of(member1)
        every { userRepository.findById(3L) } returns Optional.of(member2)
        
        // GBS 멤버십 확인 - 두 조원 모두 해당 GBS 멤버임
        val activeMember1 = GbsMemberHistory(
            id = 1L,
            member = member1,
            gbsGroup = gbsGroup,
            startDate = today.minusMonths(1),
            endDate = null
        )
        
        val activeMember2 = GbsMemberHistory(
            id = 2L,
            member = member2,
            gbsGroup = gbsGroup,
            startDate = today.minusMonths(1),
            endDate = null
        )
        
        val activeMembers = listOf(activeMember1, activeMember2)
        
        every { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        } returns activeMembers
        
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
        
        every { attendanceRepository.saveAll(any<List<Attendance>>()) } returns listOf(savedAttendance1, savedAttendance2)
        
        // when
        val result = attendanceService.createAttendances(batchRequest)
        
        // then
        assertEquals(2, result.size)
        assertEquals(2L, result[0].memberId)
        assertEquals("조원1", result[0].memberName)
        assertEquals(WorshipStatus.O, result[0].worship)
        
        verify { gbsGroupRepository.findById(1L) }
        verify { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) }
        verify { userRepository.findById(2L) }
        verify { userRepository.findById(3L) }
        verify { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        }
        verify { attendanceRepository.saveAll(any<List<Attendance>>()) }
    }
    
    @Test
    @DisplayName("출석 일괄 등록 - 권한이 없는 경우 에러 발생")
    fun testCreateAttendances_WithoutAccess() {
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(currentUser)
        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns userDetails
        SecurityContextHolder.setContext(securityContext)
        
        // given
        val batchRequest = AttendanceBatchRequest(
            gbsId = 1L,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceItemRequest(memberId = 2L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A)
            )
        )
        
        // GBS 그룹 조회
        every { gbsGroupRepository.findById(1L) } returns Optional.of(gbsGroup)
        
        // 리더 권한이 없음
        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L) } returns null
        every { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(1L, 1L, today) } returns emptyList()
        
        // when, then
        assertThrows<AccessDeniedException> {
            attendanceService.createAttendances(batchRequest)
        }
        
        verify { gbsGroupRepository.findById(1L) }
        verify { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L) }
        verify { leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(1L, 1L, today) }
    }
    
    @Test
    @DisplayName("GBS 출석 조회")
    fun testGetAttendancesByGbs() {
        // given
        val gbsId = 1L
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
        
        every { gbsGroupRepository.findById(gbsId) } returns Optional.of(gbsGroup)
        every { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) } returns listOf(attendance)
        
        // when
        val result = attendanceService.getAttendancesByGbs(gbsId, weekStart)
        
        // then
        assertEquals(1, result.size)
        assertEquals(2L, result[0].memberId)
        assertEquals("조원", result[0].memberName)
        assertEquals(WorshipStatus.O, result[0].worship)
        
        verify { gbsGroupRepository.findById(gbsId) }
        verify { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) }
    }
    
    @Test
    @DisplayName("마을 출석 현황 조회")
    fun testGetVillageAttendance() {
        // given
        val villageId = 1L
        
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
        
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, today) } returns listOf(gbsGroup)
        every { attendanceRepository.findDetailsByGbsIdAndWeek(1L, weekStart) } returns listOf(attendance)
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L) } returns leader
        every { gbsMemberHistoryRepository.countActiveMembers(1L, today) } returns 10L
        
        // when
        val result = attendanceService.getVillageAttendance(villageId, weekStart)
        
        // then
        assertEquals(villageId, result.villageId)
        assertEquals("테스트 마을", result.villageName)
        assertEquals(1, result.gbsAttendances.size)
        assertEquals(1L, result.gbsAttendances[0].gbsId)
        assertEquals("테스트 GBS", result.gbsAttendances[0].gbsName)
        assertEquals("리더", result.gbsAttendances[0].leaderName)
        assertEquals(10, result.gbsAttendances[0].totalMembers)
        assertEquals(1, result.gbsAttendances[0].attendedMembers)
        assertEquals(10.0, result.gbsAttendances[0].attendanceRate)
        
        verify { villageRepository.findById(villageId) }
        verify { gbsGroupRepository.findActiveGroupsByVillageId(villageId, today) }
        verify { attendanceRepository.findDetailsByGbsIdAndWeek(1L, weekStart) }
        verify { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L) }
        verify { gbsMemberHistoryRepository.countActiveMembers(1L, today) }
    }
    
    @Test
    @DisplayName("GBS 출석 요약 정보 생성 검증")
    fun testCreateGbsAttendanceSummary() {
        // given
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
        
        // createGbsAttendanceSummary는 private 메소드이므로 getVillageAttendance를 통해 간접적으로 테스트
        
        every { villageRepository.findById(1L) } returns Optional.of(village)
        every { gbsGroupRepository.findActiveGroupsByVillageId(1L, today) } returns listOf(gbsGroup)
        every { attendanceRepository.findDetailsByGbsIdAndWeek(1L, weekStart) } returns listOf(attendance)
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L) } returns leader
        every { gbsMemberHistoryRepository.countActiveMembers(1L, today) } returns 5L
        
        // when
        val result = attendanceService.getVillageAttendance(1L, weekStart)
        
        // then
        val gbsSummary = result.gbsAttendances.first()
        assertEquals(1L, gbsSummary.gbsId)
        assertEquals("테스트 GBS", gbsSummary.gbsName)
        assertEquals("리더", gbsSummary.leaderName)
        assertEquals(5, gbsSummary.totalMembers)
        assertEquals(1, gbsSummary.attendedMembers)
        assertEquals(20.0, gbsSummary.attendanceRate) // 1/5 = 20%
        assertEquals(1, gbsSummary.memberAttendances.size)
    }

    @Test
    @DisplayName("출석 일괄 등록 - GBS에 속하지 않은 멤버 거부")
    fun testCreateAttendances_RejectNonGbsMember() {
        // SecurityContext 모킹 설정
        val userDetails = UserDetailsAdapter(currentUser)
        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns userDetails
        SecurityContextHolder.setContext(securityContext)
        
        // given
        val nonGbsMember = User(
            id = 5L, 
            email = "nonmember@example.com", 
            password = "pwd", 
            name = "타GBS 조원", 
            role = Role.MEMBER,
            department = department
        )
        
        val batchRequest = AttendanceBatchRequest(
            gbsId = 1L,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceItemRequest(memberId = 5L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A)
            )
        )
        
        // GBS 그룹 조회
        every { gbsGroupRepository.findById(1L) } returns Optional.of(gbsGroup)
        
        // 리더 권한 확인 (권한 있음)
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            leader = currentUser,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(30)
        )
        every { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L) } returns leaderHistory
        
        // 기존 출석 데이터 조회
        every { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) } returns emptyList()
        
        // 조원 정보 조회
        every { userRepository.findById(5L) } returns Optional.of(nonGbsMember)
        
        // GBS 멤버십 확인 - 속하지 않은 멤버
        every { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        } returns emptyList() // 멤버가 없음
        
        // when & then
        assertThrows<AccessDeniedException> {
            attendanceService.createAttendances(batchRequest)
        }
        
        verify { gbsGroupRepository.findById(1L) }
        verify { gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(1L, 1L) }
        verify { attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart) }
        verify { userRepository.findById(5L) }
        verify { 
            gbsMemberHistoryRepository.findActiveMembers(
                match<GbsMemberHistorySearchCondition> { 
                    it.gbsId == 1L && it.startDate == today && it.endDate == today 
                }
            ) 
        }
    }
} 