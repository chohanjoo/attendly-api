package com.attendly.api.controller.admin

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.admin.AdminAttendanceResponse
import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
import com.attendly.api.dto.admin.AdminAttendanceStatisticsResponse
import com.attendly.enums.AttendanceStatus
import com.attendly.service.AdminAttendanceService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class AdminAttendanceControllerTest {

    @MockK
    private lateinit var adminAttendanceService: AdminAttendanceService

    @InjectMockKs
    private lateinit var adminAttendanceController: AdminAttendanceController

    private lateinit var mockPageResponse: PageResponse<AdminAttendanceResponse>
    private lateinit var mockAttendanceResponse: AdminAttendanceResponse

    @BeforeEach
    fun setup() {
        // 테스트용 응답 데이터 설정
        mockAttendanceResponse = AdminAttendanceResponse(
            id = 1L,
            userId = 2L,
            userName = "홍길동",
            date = LocalDate.now(),
            status = AttendanceStatus.PRESENT,
            eventType = "주일예배",
            note = null
        )

        mockPageResponse = PageResponse(
            items = listOf(mockAttendanceResponse),
            totalCount = 1,
            hasMore = false
        )
    }

    @Test
    fun `관리자용 출석 데이터 조회 테스트`() {
        // given
        val page = 0
        val size = 10
        val search = "홍길동"
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        val status = AttendanceStatus.PRESENT

        val expectedRequest = AdminAttendanceSearchRequest(
            page = page,
            size = size,
            search = search,
            startDate = startDate,
            endDate = endDate,
            status = status
        )

        every {
            adminAttendanceService.getAttendancesForAdmin(expectedRequest)
        } returns mockPageResponse

        // when
        val response = adminAttendanceController.getAttendances(
            page = page,
            size = size,
            search = search,
            startDate = startDate,
            endDate = endDate,
            status = status
        )

        // then
        verify {
            adminAttendanceService.getAttendancesForAdmin(expectedRequest)
        }

        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
        val responseBody = response.body
        assertNotNull(responseBody)
        assertEquals(true, responseBody.success)
        assertEquals(200, responseBody.code)
        assertNotNull(responseBody.data)
        assertEquals(1, responseBody.data!!.items.size)
        assertEquals(1, responseBody.data!!.totalCount)
        assertEquals(false, responseBody.data!!.hasMore)
        
        val attendanceData = responseBody.data!!.items.first()
        assertEquals(1L, attendanceData.id)
        assertEquals(2L, attendanceData.userId)
        assertEquals("홍길동", attendanceData.userName)
        assertEquals(LocalDate.now(), attendanceData.date)
        assertEquals(AttendanceStatus.PRESENT, attendanceData.status)
        assertEquals("주일예배", attendanceData.eventType)
    }

    @Test
    fun `파라미터 없이 전체 출석 데이터 조회 테스트`() {
        // given
        val expectedRequest = AdminAttendanceSearchRequest(
            page = 0,
            size = 20
        )

        every {
            adminAttendanceService.getAttendancesForAdmin(expectedRequest)
        } returns mockPageResponse

        // when
        val response = adminAttendanceController.getAttendances(
            page = 0,
            size = 20,
            search = null,
            startDate = null,
            endDate = null,
            status = null
        )

        // then
        verify {
            adminAttendanceService.getAttendancesForAdmin(expectedRequest)
        }

        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
    }
    
    @Test
    fun `출석 통계 정보 조회 테스트`() {
        // given
        val statisticsResponse = AdminAttendanceStatisticsResponse(
            attendanceRate = 75.0,
            attendanceRateDifference = 5.0,
            totalAttendanceCount = 150,
            absentRate = 15.0,
            absentRateDifference = -3.0,
            lateRate = 10.0,
            lateRateDifference = -2.0
        )
        
        every {
            adminAttendanceService.getAttendanceStatistics()
        } returns statisticsResponse
        
        // when
        val response = adminAttendanceController.getAttendanceStatistics()
        
        // then
        verify {
            adminAttendanceService.getAttendanceStatistics()
        }
        
        assertEquals(HttpStatus.OK.value(), response.statusCodeValue)
        val responseBody = response.body
        assertNotNull(responseBody)
        assertEquals(true, responseBody.success)
        assertEquals(200, responseBody.code)
        assertNotNull(responseBody.data)
        
        val data = responseBody.data!!
        assertEquals(75.0, data.attendanceRate)
        assertEquals(5.0, data.attendanceRateDifference)
        assertEquals(150, data.totalAttendanceCount)
        assertEquals(15.0, data.absentRate)
        assertEquals(-3.0, data.absentRateDifference)
        assertEquals(10.0, data.lateRate)
        assertEquals(-2.0, data.lateRateDifference)
    }
} 