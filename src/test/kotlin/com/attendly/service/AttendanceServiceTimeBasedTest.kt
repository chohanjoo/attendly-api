package com.attendly.service

import com.attendly.api.dto.VillageAttendanceResponse
import com.attendly.domain.entity.*
import com.attendly.domain.repository.*
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AttendanceServiceTimeBasedTest {

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

    @InjectMockKs
    private lateinit var attendanceService: AttendanceService

    // 테스트에 사용할 기본 데이터
    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup1: GbsGroup
    private lateinit var gbsGroup2: GbsGroup
    private lateinit var leader1Past: User
    private lateinit var leader1Current: User
    private lateinit var leader2: User
    private lateinit var member1: User
    private lateinit var member2: User
    private lateinit var member3: User
    private lateinit var member4: User

    // 테스트에 사용할 날짜
    private val pastDate = LocalDate.of(2023, 1, 8) // 과거 날짜 (2023년 1월 8일)
    private val currentDate = LocalDate.now() // 현재 날짜

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
            id = 2L,
            name = "R village",
            department = department
        )
        
        // GBS 그룹 생성 (과거와 현재 모두 활성화된 그룹)
        gbsGroup1 = GbsGroup(
            id = 2L,
            name = "GBS1",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2024, 12, 31)
        )
        
        // 과거에만 활성화된 GBS 그룹
        gbsGroup2 = GbsGroup(
            id = 3L,
            name = "GBS2",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 6, 30)
        )
        
        // 사용자들 생성
        leader1Past = User(
            id = 10L,
            email = "leader1_past@example.com",
            password = "password",
            name = "과거리더1",
            role = Role.LEADER,
            department = department
        )
        
        leader1Current = User(
            id = 11L,
            email = "leader1_current@example.com",
            password = "password",
            name = "현재리더1",
            role = Role.LEADER,
            department = department
        )
        
        leader2 = User(
            id = 12L,
            email = "leader2@example.com",
            password = "password",
            name = "리더2",
            role = Role.LEADER,
            department = department
        )
        
        member1 = User(
            id = 13L,
            email = "member1@example.com",
            password = "password",
            name = "조원1",
            role = Role.MEMBER,
            department = department
        )
        
        member2 = User(
            id = 14L,
            email = "member2@example.com",
            password = "password",
            name = "조원2",
            role = Role.MEMBER,
            department = department
        )
        
        member3 = User(
            id = 15L,
            email = "member3@example.com",
            password = "password",
            name = "조원3",
            role = Role.MEMBER,
            department = department
        )
        
        member4 = User(
            id = 16L,
            email = "member4@example.com",
            password = "password",
            name = "조원4",
            role = Role.MEMBER,
            department = department
        )
    }

    @Test
    @DisplayName("과거 시점의 마을 출석 현황을 조회할 수 있다")
    fun testGetVillageAttendanceForPastDate() {
        // given
        val villageId = 2L
        val weekStart = pastDate
        
        // 1. 마을 정보 조회
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        
        // 2. 해당 날짜에 활성화된 GBS 그룹 조회 (과거 날짜 기준)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, weekStart) } returns listOf(gbsGroup1, gbsGroup2)
        
        // 3. GBS1의 과거 날짜 기준 리더와 출석 데이터
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, weekStart) } returns leader1Past
        
        val gbs1Member1Attendance = Attendance(
            id = 1L,
            member = member1,
            gbsGroup = gbsGroup1,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = leader1Past
        )
        
        val gbs1Member2Attendance = Attendance(
            id = 2L,
            member = member2,
            gbsGroup = gbsGroup1,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 4,
            ministry = MinistryStatus.B,
            createdBy = leader1Past
        )
        
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, weekStart) } returns 
            listOf(gbs1Member1Attendance, gbs1Member2Attendance)
        
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, weekStart) } returns 2L
        
        // 4. GBS2의 과거 날짜 기준 리더와 출석 데이터
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup2.id!!, weekStart) } returns leader2
        
        val gbs2Member3Attendance = Attendance(
            id = 3L,
            member = member3,
            gbsGroup = gbsGroup2,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 6,
            ministry = MinistryStatus.A,
            createdBy = leader2
        )
        
        val gbs2Member4Attendance = Attendance(
            id = 4L,
            member = member4,
            gbsGroup = gbsGroup2,
            weekStart = weekStart,
            worship = WorshipStatus.X,
            qtCount = 3,
            ministry = MinistryStatus.C,
            createdBy = leader2
        )
        
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup2.id!!, weekStart) } returns 
            listOf(gbs2Member3Attendance, gbs2Member4Attendance)
        
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup2.id!!, weekStart) } returns 2L
        
        // when
        val result: VillageAttendanceResponse = attendanceService.getVillageAttendance(villageId, weekStart)
        
        // then
        // 1. 기본 응답 검증
        assertEquals(villageId, result.villageId)
        assertEquals("R village", result.villageName)
        assertEquals(weekStart, result.weekStart)
        
        // 2. GBS 목록 검증
        assertEquals(2, result.gbsAttendances.size)
        
        // 3. GBS1 데이터 검증
        val gbs1Data = result.gbsAttendances.find { it.gbsId == 2L }!!
        assertEquals("GBS1", gbs1Data.gbsName)
        assertEquals("과거리더1", gbs1Data.leaderName) // 과거 리더 이름 확인
        assertEquals(2, gbs1Data.totalMembers)
        assertEquals(2, gbs1Data.attendedMembers)
        assertEquals(100.0, gbs1Data.attendanceRate)
        assertEquals(2, gbs1Data.memberAttendances.size)
        
        // 4. GBS2 데이터 검증
        val gbs2Data = result.gbsAttendances.find { it.gbsId == 3L }!!
        assertEquals("GBS2", gbs2Data.gbsName)
        assertEquals("리더2", gbs2Data.leaderName)
        assertEquals(2, gbs2Data.totalMembers)
        assertEquals(2, gbs2Data.attendedMembers) // 출석 기록이 있는 멤버는 2명 (WorshipStatus에 관계없이)
        assertEquals(100.0, gbs2Data.attendanceRate) // 2/2 = 100% 
        assertEquals(2, gbs2Data.memberAttendances.size)
        
        // 5. 호출 검증
        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, weekStart) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, weekStart) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup2.id!!, weekStart) }
        verify(exactly = 1) { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, weekStart) }
        verify(exactly = 1) { gbsMemberHistoryRepository.countActiveMembers(gbsGroup2.id!!, weekStart) }
        verify(exactly = 1) { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, weekStart) }
        verify(exactly = 1) { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup2.id!!, weekStart) }
    }

    @Test
    @DisplayName("GBS 리더 변경 이력이 있는 경우 해당 시점의 리더 정보를 조회한다")
    fun testLeaderChangeOverTime() {
        // given
        val villageId = 2L
        
        // 1. 마을 정보 조회 - 모든 테스트 케이스에 공통
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        
        // 2. 과거 시점 - 과거 리더로 셋업
        val pastDate = LocalDate.of(2023, 1, 8)
        
        // 과거 시점에는 GBS1, GBS2 모두 활성화
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, pastDate) } returns listOf(gbsGroup1, gbsGroup2)
        // 과거 시점의 리더 설정
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, pastDate) } returns leader1Past
        // 간단한 출석 데이터와 멤버 수 설정 (상세 검증은 하지 않을 예정)
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, pastDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, pastDate) } returns 2L
        
        // GBS2 관련 설정 (간단하게만 설정)
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup2.id!!, pastDate) } returns leader2
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup2.id!!, pastDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup2.id!!, pastDate) } returns 2L
        
        // 3. 현재 시점 - 현재 리더로 셋업
        val currentDate = LocalDate.now()
        
        // 현재 시점에는 GBS1만 활성화 (GBS2는 이미 종료됨)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) } returns listOf(gbsGroup1)
        // 현재 시점의 리더 설정
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, currentDate) } returns leader1Current
        // 간단한 출석 데이터와 멤버 수 설정
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, currentDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, currentDate) } returns 3L // 멤버 수 변경
        
        // when - 과거 시점 조회
        val pastResult = attendanceService.getVillageAttendance(villageId, pastDate)
        
        // then - 과거 시점 검증
        assertEquals("R village", pastResult.villageName)
        assertEquals(2, pastResult.gbsAttendances.size) // 과거에는 GBS1, GBS2 모두 있음
        
        val pastGbs1Data = pastResult.gbsAttendances.find { it.gbsId == 2L }!!
        assertEquals("과거리더1", pastGbs1Data.leaderName) // 과거 리더 이름
        assertEquals(2, pastGbs1Data.totalMembers) // 과거 멤버 수
        
        // when - 현재 시점 조회
        val currentResult = attendanceService.getVillageAttendance(villageId, currentDate)
        
        // then - 현재 시점 검증
        assertEquals(1, currentResult.gbsAttendances.size) // 현재는 GBS1만 활성화됨
        
        val currentGbs1Data = currentResult.gbsAttendances.find { it.gbsId == 2L }!!
        assertEquals("현재리더1", currentGbs1Data.leaderName) // 현재 리더 이름
        assertEquals(3, currentGbs1Data.totalMembers) // 현재 멤버 수
        
        // 호출 횟수 검증
        verify(exactly = 2) { villageRepository.findById(villageId) } // 두 번 호출
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, pastDate) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, pastDate) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, currentDate) }
    }

    @Test
    @DisplayName("GBS 멤버 변경 이력이 있는 경우 해당 시점의 멤버 수를 정확히 계산한다")
    fun testMemberChangeOverTime() {
        // given
        val villageId = 2L
        val gbsId = 2L
        
        // 1. 마을 정보 조회
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        
        // 2. 시점별 데이터 준비
        // 과거 시점 1 (2023-01-08) - 멤버 2명
        val date1 = LocalDate.of(2023, 1, 8)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, date1) } returns listOf(gbsGroup1)
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsId, date1) } returns leader1Past
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, date1) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsId, date1) } returns 2L
        
        // 과거 시점 2 (2023-06-01) - 멤버 3명 (증가)
        val date2 = LocalDate.of(2023, 6, 1)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, date2) } returns listOf(gbsGroup1)
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsId, date2) } returns leader1Past
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, date2) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsId, date2) } returns 3L
        
        // 현재 시점 - 멤버 4명 (다시 증가)
        val currentDate = LocalDate.now()
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) } returns listOf(gbsGroup1)
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsId, currentDate) } returns leader1Current
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsId, currentDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsId, currentDate) } returns 4L
        
        // when
        val result1 = attendanceService.getVillageAttendance(villageId, date1)
        val result2 = attendanceService.getVillageAttendance(villageId, date2)
        val result3 = attendanceService.getVillageAttendance(villageId, currentDate)
        
        // then
        // 시점 1 검증
        val gbs1Data1 = result1.gbsAttendances.first()
        assertEquals(2, gbs1Data1.totalMembers)
        assertEquals("과거리더1", gbs1Data1.leaderName)
        
        // 시점 2 검증
        val gbs1Data2 = result2.gbsAttendances.first()
        assertEquals(3, gbs1Data2.totalMembers)
        assertEquals("과거리더1", gbs1Data2.leaderName)
        
        // 시점 3 검증 (현재)
        val gbs1Data3 = result3.gbsAttendances.first()
        assertEquals(4, gbs1Data3.totalMembers)
        assertEquals("현재리더1", gbs1Data3.leaderName)
        
        // 검증
        verify(exactly = 3) { villageRepository.findById(villageId) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, date1) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, date2) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) }
        verify(exactly = 1) { gbsMemberHistoryRepository.countActiveMembers(gbsId, date1) }
        verify(exactly = 1) { gbsMemberHistoryRepository.countActiveMembers(gbsId, date2) }
        verify(exactly = 1) { gbsMemberHistoryRepository.countActiveMembers(gbsId, currentDate) }
    }

    @Test
    @DisplayName("시간에 따른 데이터 변화를 고려한 마을 출석 현황 조회 테스트")
    fun testGetVillageAttendanceOverTime() {
        // given
        val villageId = 2L
        
        // 1. 마을 정보 조회
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        
        // 2. 과거 시점 - 과거 리더로 셋업
        val pastDate = LocalDate.of(2023, 1, 8)
        
        // 과거 시점에는 GBS1, GBS2 모두 활성화
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, pastDate) } returns listOf(gbsGroup1, gbsGroup2)
        // 과거 시점의 리더 설정
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, pastDate) } returns leader1Past
        // 간단한 출석 데이터와 멤버 수 설정 (상세 검증은 하지 않을 예정)
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, pastDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, pastDate) } returns 2L
        
        // GBS2 관련 설정 (간단하게만 설정)
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup2.id!!, pastDate) } returns leader2
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup2.id!!, pastDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup2.id!!, pastDate) } returns 2L
        
        // 3. 현재 시점 - 현재 리더로 셋업
        val currentDate = LocalDate.now()
        
        // 현재 시점에는 GBS1만 활성화 (GBS2는 이미 종료됨)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) } returns listOf(gbsGroup1)
        // 현재 시점의 리더 설정
        every { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, currentDate) } returns leader1Current
        // 간단한 출석 데이터와 멤버 수 설정
        every { attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, currentDate) } returns listOf()
        every { gbsMemberHistoryRepository.countActiveMembers(gbsGroup1.id!!, currentDate) } returns 3L // 멤버 수 변경
        
        // when - 과거 시점 조회
        val pastResult = attendanceService.getVillageAttendance(villageId, pastDate)
        
        // then - 과거 시점 검증
        assertEquals("R village", pastResult.villageName)
        assertEquals(2, pastResult.gbsAttendances.size) // 과거에는 GBS1, GBS2 모두 있음
        
        val pastGbs1Data = pastResult.gbsAttendances.find { it.gbsId == 2L }!!
        assertEquals("과거리더1", pastGbs1Data.leaderName) // 과거 리더 이름
        assertEquals(2, pastGbs1Data.totalMembers) // 과거 멤버 수
        
        // when - 현재 시점 조회
        val currentResult = attendanceService.getVillageAttendance(villageId, currentDate)
        
        // then - 현재 시점 검증
        assertEquals(1, currentResult.gbsAttendances.size) // 현재는 GBS1만 활성화됨
        
        val currentGbs1Data = currentResult.gbsAttendances.find { it.gbsId == 2L }!!
        assertEquals("현재리더1", currentGbs1Data.leaderName) // 현재 리더 이름
        assertEquals(3, currentGbs1Data.totalMembers) // 현재 멤버 수
        
        // 호출 횟수 검증
        verify(exactly = 2) { villageRepository.findById(villageId) } // 두 번 호출
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, pastDate) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, pastDate) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findLeaderByGbsIdAndDate(gbsGroup1.id!!, currentDate) }
    }
}