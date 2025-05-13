package com.attendly.service

import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
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
import org.springframework.data.domain.Sort
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AdminAttendanceServiceTest {

    @MockK
    private lateinit var attendanceRepository: AttendanceRepository

    @InjectMockKs
    private lateinit var adminAttendanceService: AdminAttendanceService

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var leader: User
    private lateinit var member: User
    private lateinit var attendance: Attendance

    @BeforeEach
    fun setUp() {
        val now = LocalDate.now()
        
        department = Department(id = 1L, name = "대학부")
        village = Village(id = 1L, name = "1마을", department = department)
        gbsGroup = GbsGroup(
            id = 1L,
            name = "믿음 GBS",
            village = village,
            termStartDate = now.minusMonths(1),
            termEndDate = now.plusMonths(5)
        )
        
        leader = User(
            id = 1L, 
            name = "리더", 
            role = Role.LEADER, 
            department = department
        )
        
        member = User(
            id = 2L, 
            name = "홍길동", 
            role = Role.MEMBER, 
            department = department
        )
        
        attendance = Attendance(
            id = 1L,
            member = member,
            gbsGroup = gbsGroup,
            weekStart = now,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.A,
            createdBy = leader
        )
    }

    @Test
    fun `출석 목록 정상 조회 테스트`() {
        // given
        val request = AdminAttendanceSearchRequest(
            page = 0,
            size = 10,
            search = "홍길동",
            startDate = LocalDate.now().minusDays(7),
            endDate = LocalDate.now(),
            status = AttendanceStatus.PRESENT
        )
        
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "weekStart")
        )
        
        val attendancesPage = PageImpl(
            listOf(attendance),
            pageable,
            1
        )
        
        every {
            attendanceRepository.findAttendancesForAdmin(
                search = request.search,
                startDate = request.startDate,
                endDate = request.endDate,
                status = request.status,
                pageable = pageable
            )
        } returns attendancesPage
        
        // when
        val result = adminAttendanceService.getAttendancesForAdmin(request)
        
        // then
        verify {
            attendanceRepository.findAttendancesForAdmin(
                search = request.search,
                startDate = request.startDate,
                endDate = request.endDate,
                status = request.status,
                pageable = pageable
            )
        }
        
        assertEquals(1, result.items.size)
        assertEquals(1, result.totalCount)
        assertFalse(result.hasMore)
        
        val attendanceResponse = result.items.first()
        assertEquals(1L, attendanceResponse.id)
        assertEquals(2L, attendanceResponse.userId)
        assertEquals("홍길동", attendanceResponse.userName)
        assertEquals(LocalDate.now(), attendanceResponse.date)
        assertEquals(AttendanceStatus.PRESENT, attendanceResponse.status)
        assertEquals("주일예배", attendanceResponse.eventType)
    }

    @Test
    fun `출석 없음 조회 테스트`() {
        // given
        val request = AdminAttendanceSearchRequest(
            page = 0,
            size = 10
        )
        
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "weekStart")
        )
        
        val emptyPage = PageImpl<Attendance>(
            emptyList(),
            pageable,
            0
        )
        
        every {
            attendanceRepository.findAttendancesForAdmin(
                search = null,
                startDate = null,
                endDate = null,
                status = null,
                pageable = pageable
            )
        } returns emptyPage
        
        // when
        val result = adminAttendanceService.getAttendancesForAdmin(request)
        
        // then
        verify {
            attendanceRepository.findAttendancesForAdmin(
                search = null,
                startDate = null,
                endDate = null,
                status = null,
                pageable = pageable
            )
        }
        
        assertEquals(0, result.items.size)
        assertEquals(0, result.totalCount)
        assertFalse(result.hasMore)
    }
} 