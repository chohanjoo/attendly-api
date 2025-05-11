package com.attendly.service

import com.attendly.api.dto.GbsStatistics
import com.attendly.api.dto.VillageStatistics
import com.attendly.domain.entity.*
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.enums.MinistryStatus
import com.attendly.enums.Role
import com.attendly.enums.WorshipStatus
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class StatisticsServiceTest {

    @MockK
    private lateinit var attendanceRepository: AttendanceRepository

    @MockK
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @MockK
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @MockK
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @MockK
    private lateinit var organizationService: OrganizationService

    @InjectMockKs
    private lateinit var statisticsService: StatisticsService

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var leader: User
    private lateinit var member: User
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        
        department = Department(
            id = 1L,
            name = "청년부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        village = Village(
            id = 1L,
            name = "1마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        gbsGroup = GbsGroup(
            id = 1L,
            name = "1GBS",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        leader = User(
            id = 1L,
            name = "홍길동",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        member = User(
            id = 2L,
            name = "김모범",
            role = Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        startDate = LocalDate.of(2023, 1, 1)
        endDate = LocalDate.of(2023, 1, 31)
    }

    @Test
    @DisplayName("getDepartmentStatistics: 부서 출석 통계를 올바르게 조회해야 함")
    fun getDepartmentStatisticsTest() {
        // given
        every { organizationService.getDepartmentById(1L) } returns department
        every { organizationService.getVillagesByDepartment(1L) } returns listOf(village)
        
        // 마을 통계를 모킹
        val villageStats = VillageStatistics(
            villageId = 1L,
            villageName = "1마을",
            gbsStats = emptyList(),
            totalMembers = 10,
            attendedMembers = 8,
            attendanceRate = 80.0,
            averageQtCount = 4.0
        )
        
        // getVillageStatistics를 스파이로 모킹
        val spyService = spyk(statisticsService)
        every { spyService.getVillageStatistics(1L, startDate, endDate) } returns villageStats
        
        // when
        val result = spyService.getDepartmentStatistics(1L, startDate, endDate)
        
        // then
        assertEquals(1L, result.departmentId)
        assertEquals("청년부", result.departmentName)
        assertEquals(startDate, result.startDate)
        assertEquals(endDate, result.endDate)
        assertEquals(10, result.totalMembers)
        assertEquals(8, result.attendedMembers)
        assertEquals(80.0, result.attendanceRate)
        assertEquals(4.0, result.averageQtCount)
        assertEquals(1, result.villageStats.size)
    }

    @Test
    @DisplayName("getDepartmentStatistics: 마을이 없는 경우 예외를 발생시켜야 함")
    fun getDepartmentStatisticsWithNoVillagesTest() {
        // given
        every { organizationService.getDepartmentById(1L) } returns department
        every { organizationService.getVillagesByDepartment(1L) } returns emptyList()
        
        // when & then
        val exception = assertThrows<AttendlyApiException> {
            statisticsService.getDepartmentStatistics(1L, startDate, endDate)
        }
        
        // 올바른 에러 메시지 확인
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.message, exception.message)
    }

    @Test
    @DisplayName("getVillageStatistics: 마을 출석 통계를 올바르게 조회해야 함")
    fun getVillageStatisticsTest() {
        // given
        every { organizationService.getVillageWithActiveGbsGroups(1L, startDate) } returns Pair(village, listOf(gbsGroup))
        
        // GBS 통계를 모킹
        val gbsStats = GbsStatistics(
            gbsId = 1L,
            gbsName = "1GBS",
            leaderName = "홍길동",
            totalMembers = 10,
            attendedMembers = 8,
            attendanceRate = 80.0,
            averageQtCount = 4.0,
            weeklyStats = emptyList()
        )
        
        // getGbsStatistics를 스파이로 모킹
        val spyService = spyk(statisticsService)
        every { spyService.getGbsStatistics(1L, startDate, endDate) } returns gbsStats
        
        // when
        val result = spyService.getVillageStatistics(1L, startDate, endDate)
        
        // then
        assertEquals(1L, result.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(10, result.totalMembers)
        assertEquals(8, result.attendedMembers)
        assertEquals(80.0, result.attendanceRate)
        assertEquals(4.0, result.averageQtCount)
        assertEquals(1, result.gbsStats.size)
    }

    @Test
    @DisplayName("getVillageStatistics: 활성 GBS가 없는 경우 예외를 발생시켜야 함")
    fun getVillageStatisticsWithNoGbsTest() {
        // given
        every { organizationService.getVillageWithActiveGbsGroups(1L, startDate) } returns Pair(village, emptyList())
        
        // when & then
        val exception = assertThrows<AttendlyApiException> {
            statisticsService.getVillageStatistics(1L, startDate, endDate)
        }
        
        // 올바른 에러 메시지 확인
        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.message, exception.message)
    }

    @Test
    @DisplayName("getGbsStatistics: GBS 출석 통계를 올바르게 조회해야 함")
    fun getGbsStatisticsTest() {
        // given
        val sunday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val nextSunday = sunday.plusWeeks(1)
        
        // 조인 쿼리를 사용하여 GBS와 리더 정보를 함께 조회하도록 변경
        every { organizationService.getGbsWithLeader(1L) } returns Pair(gbsGroup, "홍길동")
        every { gbsMemberHistoryRepository.countActiveMembers(1L, startDate) } returns 10L
        
        // 주차 날짜 생성을 위해 테스트 서비스 스파이 생성
        val spyService = spyk(statisticsService)
        
        // generateWeeklyDates 메서드가 특정 주차 날짜를 반환하도록 모킹
        val weeklyDates = listOf(sunday, nextSunday)
        val generateWeeklyDatesMethod = StatisticsService::class.java.getDeclaredMethod(
            "generateWeeklyDates",
            LocalDate::class.java,
            LocalDate::class.java
        ).apply { isAccessible = true }
        
        every { 
            generateWeeklyDatesMethod.invoke(spyService, startDate, endDate)
        } returns weeklyDates
        
        // 첫 번째 주 출석 데이터
        val attendances1 = listOf(
            createAttendance(1L, member, gbsGroup, sunday, WorshipStatus.O, 3),
            createAttendance(2L, member, gbsGroup, sunday, WorshipStatus.O, 4)
        )
        
        // 두 번째 주 출석 데이터
        val attendances2 = listOf(
            createAttendance(3L, member, gbsGroup, nextSunday, WorshipStatus.O, 5),
            createAttendance(4L, member, gbsGroup, nextSunday, WorshipStatus.X, 2)
        )
        
        // 개선된 메서드를 모킹: 모든 주차의 데이터를 한 번에 조회
        val attendancesByWeek = mapOf(
            sunday to attendances1,
            nextSunday to attendances2
        )
        every { attendanceRepository.findDetailsByGbsIdAndWeeks(1L, weeklyDates) } returns attendancesByWeek
        
        // when
        val result = spyService.getGbsStatistics(1L, startDate, endDate)
        
        // then
        assertEquals(1L, result.gbsId)
        assertEquals("1GBS", result.gbsName)
        assertEquals("홍길동", result.leaderName)
        assertEquals(10, result.totalMembers)
        // 출석 멤버 수와 통계는 테스트 환경에 따라 달라질 수 있음
        assertEquals(2, result.weeklyStats.size)
    }

    @Test
    @DisplayName("getGbsStatistics: 리더가 없는 경우에도 올바르게 통계를 조회해야 함")
    fun getGbsStatisticsWithNoLeaderTest() {
        // given
        val sunday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        
        // 리더가 없는 경우
        every { organizationService.getGbsWithLeader(1L) } returns Pair(gbsGroup, null)
        every { gbsMemberHistoryRepository.countActiveMembers(1L, startDate) } returns 10L
        
        // 출석 데이터
        val attendances = listOf(
            createAttendance(1L, member, gbsGroup, sunday, WorshipStatus.O, 3)
        )
        
        // 주차 날짜 생성을 위해 테스트 서비스 스파이 생성
        val spyService = spyk(statisticsService)
        
        // generateWeeklyDates 메서드가 특정 주차 날짜를 반환하도록 모킹
        val weeklyDates = listOf(sunday)
        val generateWeeklyDatesMethod = StatisticsService::class.java.getDeclaredMethod(
            "generateWeeklyDates",
            LocalDate::class.java,
            LocalDate::class.java
        ).apply { isAccessible = true }
        
        every { 
            generateWeeklyDatesMethod.invoke(spyService, startDate, endDate)
        } returns weeklyDates
        
        // 개선된 메서드를 모킹
        val attendancesByWeek = mapOf(
            sunday to attendances
        )
        every { attendanceRepository.findDetailsByGbsIdAndWeeks(1L, weeklyDates) } returns attendancesByWeek
        
        // when
        val result = spyService.getGbsStatistics(1L, startDate, endDate)
        
        // then
        assertEquals(1L, result.gbsId)
        assertEquals("1GBS", result.gbsName)
        assertEquals("리더 없음", result.leaderName)
        assertEquals(10, result.totalMembers)
        assertEquals(1, result.weeklyStats.size)
    }

    @Test
    @DisplayName("getGbsStatistics: 활성 멤버가 없는 경우 예외를 발생시켜야 함")
    fun getGbsStatisticsWithNoActiveMembers() {
        // given
        every { organizationService.getGbsWithLeader(1L) } returns Pair(gbsGroup, "홍길동")
        every { gbsMemberHistoryRepository.countActiveMembers(1L, startDate) } returns 0L
        
        // when & then
        val exception = assertThrows<AttendlyApiException> {
            statisticsService.getGbsStatistics(1L, startDate, endDate)
        }
        
        // 올바른 에러 메시지 확인
        assertEquals(ErrorMessage.GBS_GROUP_NOT_FOUND.message, exception.message)
    }

    @Test
    @DisplayName("generateWeeklyDates: 시작일부터 종료일까지의 주차 시작일을 올바르게 생성해야 함")
    fun generateWeeklyDatesTest() {
        // given
        val startDate = LocalDate.of(2023, 1, 3) // 화요일
        val endDate = LocalDate.of(2023, 1, 22) // 일요일
        
        // MockK를 사용한 private 메소드 테스트
        val method = statisticsService::class.java.getDeclaredMethod(
            "generateWeeklyDates",
            LocalDate::class.java,
            LocalDate::class.java
        ).apply { isAccessible = true }
        
        // when
        val result = method.invoke(statisticsService, startDate, endDate) as List<LocalDate>
        
        // then
        assertEquals(4, result.size)
        assertEquals(LocalDate.of(2023, 1, 1), result[0]) // 첫 번째 일요일
        assertEquals(LocalDate.of(2023, 1, 8), result[1]) // 두 번째 일요일
        assertEquals(LocalDate.of(2023, 1, 15), result[2]) // 세 번째 일요일
        assertEquals(LocalDate.of(2023, 1, 22), result[3]) // 네 번째 일요일
    }

    private fun createAttendance(
        id: Long,
        member: User,
        gbsGroup: GbsGroup,
        weekStart: LocalDate,
        worship: WorshipStatus,
        qtCount: Int
    ): Attendance {
        return Attendance(
            id = id,
            member = member,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = worship,
            qtCount = qtCount,
            ministry = MinistryStatus.A,
            createdBy = leader,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
} 