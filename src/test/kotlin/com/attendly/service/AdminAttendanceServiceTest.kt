package com.attendly.service

import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
import com.attendly.api.dto.admin.AdminAttendanceStatisticsResponse
import com.attendly.domain.entity.Attendance
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.enums.AttendanceStatus
import com.attendly.enums.MinistryStatus
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import com.attendly.enums.WorshipStatus
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AdminAttendanceServiceTest {

    @MockK
    lateinit var attendanceRepository: AttendanceRepository

    @InjectMockKs
    lateinit var adminAttendanceService: AdminAttendanceService

    private lateinit var user: User
    private lateinit var gbsGroup: GbsGroup
    private lateinit var attendance: Attendance
    private lateinit var currentMonth: LocalDate
    private lateinit var prevMonth: LocalDate

    @BeforeEach
    fun setup() {
        // 테스트에 필요한 데이터 세팅
        val department = Department(id = 1L, name = "테스트학과")
        val village = Village(id = 1L, name = "테스트마을", department = department)
        
        val now = LocalDate.now()
        gbsGroup = GbsGroup(
            id = 1L, 
            name = "테스트GBS", 
            village = village,
            termStartDate = now.minusMonths(1),
            termEndDate = now.plusMonths(5)
        )
        
        user = User(
            id = 1L,
            email = "test@example.com",
            name = "테스트유저",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department
        )
        
        currentMonth = LocalDate.now()
        prevMonth = currentMonth.minusMonths(1)
        
        attendance = Attendance(
            id = 1L,
            member = user,
            gbsGroup = gbsGroup,
            weekStart = currentMonth,
            worship = WorshipStatus.O,
            qtCount = 7,
            ministry = MinistryStatus.A,
            createdBy = user
        )
    }

    @Test
    fun `getAttendancesForAdmin should return attendances by filter`() {
        // Given
        val request = AdminAttendanceSearchRequest(
            page = 0,
            size = 10,
            search = "테스트",
            startDate = LocalDate.now().minusDays(7),
            endDate = LocalDate.now(),
            status = AttendanceStatus.PRESENT
        )
        
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "weekStart")
        )
        
        val attendances = listOf(attendance)
        val page = PageImpl(attendances, pageable, attendances.size.toLong())
        
        every { 
            attendanceRepository.findAttendancesForAdmin(
                request.search,
                request.startDate,
                request.endDate,
                request.status,
                pageable
            ) 
        } returns page
        
        // When
        val result = adminAttendanceService.getAttendancesForAdmin(request)
        
        // Then
        assertEquals(1, result.items.size)
        assertEquals(1, result.totalCount)
        assertFalse(result.hasMore)
        verify { 
            attendanceRepository.findAttendancesForAdmin(
                request.search,
                request.startDate,
                request.endDate,
                request.status,
                pageable
            ) 
        }
    }
    
    @Test
    fun `getAttendanceStatistics should return correct statistics`() {
        // Given
        val now = LocalDate.now()
        val currentMonthStart = now.with(TemporalAdjusters.firstDayOfMonth())
        val currentMonthEnd = now.with(TemporalAdjusters.lastDayOfMonth())
        val prevMonthStart = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
        val prevMonthEnd = now.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
        
        // 테스트 사용자 생성
        val department = Department(id = 1L, name = "테스트학과")
        val village = Village(id = 1L, name = "테스트마을", department = department)
        
        val user1 = User(
            id = 1L,
            email = "user1@example.com",
            name = "유저1",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department
        )
        
        val user2 = User(
            id = 2L,
            email = "user2@example.com",
            name = "유저2",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department
        )
        
        val user3 = User(
            id = 3L,
            email = "user3@example.com",
            name = "유저3",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department
        )
        
        val user4 = User(
            id = 4L,
            email = "user4@example.com",
            name = "유저4",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department
        )
        
        val testGbsGroup = GbsGroup(
            id = 1L, 
            name = "테스트GBS", 
            village = village,
            termStartDate = now.minusMonths(1),
            termEndDate = now.plusMonths(5)
        )
        
        // 현재 월 출석 데이터 생성
        val currentMonth1 = Attendance(
            id = 1L,
            member = user1,
            gbsGroup = testGbsGroup,
            weekStart = currentMonthStart.plusDays(1),
            worship = WorshipStatus.O,
            qtCount = 7,
            ministry = MinistryStatus.A, // PRESENT
            createdBy = user1
        )
        
        val currentMonth2 = Attendance(
            id = 2L,
            member = user2,
            gbsGroup = testGbsGroup,
            weekStart = currentMonthStart.plusDays(1),
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.B, // LATE
            createdBy = user1
        )
        
        val currentMonth3 = Attendance(
            id = 3L,
            member = user3,
            gbsGroup = testGbsGroup,
            weekStart = currentMonthStart.plusDays(1),
            worship = WorshipStatus.X,
            qtCount = 0,
            ministry = MinistryStatus.C, // ABSENT
            createdBy = user1
        )
        
        val currentMonth4 = Attendance(
            id = 4L,
            member = user4,
            gbsGroup = testGbsGroup,
            weekStart = currentMonthStart.plusDays(1),
            worship = WorshipStatus.X,
            qtCount = 3,
            ministry = MinistryStatus.C, // EXCUSED
            createdBy = user1
        )
        
        // 이전 월 출석 데이터 생성
        val prevMonth1 = Attendance(
            id = 5L,
            member = user1,
            gbsGroup = testGbsGroup,
            weekStart = prevMonthStart.plusDays(1),
            worship = WorshipStatus.O,
            qtCount = 7,
            ministry = MinistryStatus.A, // PRESENT
            createdBy = user1
        )
        
        val prevMonth2 = Attendance(
            id = 6L,
            member = user2,
            gbsGroup = testGbsGroup,
            weekStart = prevMonthStart.plusDays(1),
            worship = WorshipStatus.X,
            qtCount = 0,
            ministry = MinistryStatus.C, // ABSENT
            createdBy = user1
        )
        
        val currentMonthAttendances = listOf(currentMonth1, currentMonth2, currentMonth3, currentMonth4)
        val prevMonthAttendances = listOf(prevMonth1, prevMonth2)
        
        val currentMonthPage = PageImpl(currentMonthAttendances)
        val prevMonthPage = PageImpl(prevMonthAttendances)
        
        every { 
            attendanceRepository.findAttendancesForAdmin(
                null,
                currentMonthStart,
                currentMonthEnd,
                null,
                any()
            ) 
        } returns currentMonthPage
        
        every { 
            attendanceRepository.findAttendancesForAdmin(
                null,
                prevMonthStart,
                prevMonthEnd,
                null,
                any()
            ) 
        } returns prevMonthPage
        
        // When
        val result = adminAttendanceService.getAttendanceStatistics()
        
        // Then
        // 현재 월: 출석 1/4(25%), 지각 1/4(25%), 결석 2/4(50%)
        // 이전 월: 출석 1/2(50%), 지각 0/2(0%), 결석 1/2(50%)
        assertEquals(25.0, result.attendanceRate, 0.1)
        assertEquals(-25.0, result.attendanceRateDifference, 0.1)
        assertEquals(1, result.totalAttendanceCount)
        assertEquals(50.0, result.absentRate, 0.1)
        assertEquals(0.0, result.absentRateDifference, 0.1)
        assertEquals(25.0, result.lateRate, 0.1)
        assertEquals(25.0, result.lateRateDifference, 0.1)
        
        verify {
            attendanceRepository.findAttendancesForAdmin(
                null,
                currentMonthStart,
                currentMonthEnd,
                null,
                any()
            )
            attendanceRepository.findAttendancesForAdmin(
                null,
                prevMonthStart,
                prevMonthEnd,
                null,
                any()
            )
        }
    }
} 