package com.attendly.api.controller

import com.attendly.api.dto.AttendanceMemberItemDto
import com.attendly.api.dto.AttendanceResponse
import com.attendly.api.dto.AttendanceUpdateRequestDto
import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.VillageGbsInfoResponse
import com.attendly.enums.MinistryStatus
import com.attendly.enums.WorshipStatus
import com.attendly.service.AttendanceService
import com.attendly.service.OrganizationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class VillageLeaderControllerTest {

    private lateinit var controller: VillageLeaderController
    private lateinit var organizationService: OrganizationService
    private lateinit var attendanceService: AttendanceService

    @BeforeEach
    fun setup() {
        organizationService = mockk()
        attendanceService = mockk()
        controller = VillageLeaderController(organizationService, attendanceService)
    }

    @Test
    fun `마을의 모든 GBS 정보 조회`() {
        // given
        val villageId = 1L
        val date = LocalDate.now()
        val member1 = GbsMemberResponse(
            id = 1L,
            name = "김조원",
            email = "member1@example.com",
            birthDate = null,
            joinDate = LocalDate.of(2023, 1, 1),
            phoneNumber = "010-1234-5678"
        )
        val member2 = GbsMemberResponse(
            id = 2L,
            name = "이조원",
            email = "member2@example.com",
            birthDate = null,
            joinDate = LocalDate.of(2023, 1, 1),
            phoneNumber = "010-2345-6789"
        )
        
        val gbs1 = VillageGbsInfoResponse.GbsInfo(
            gbsId = 1L,
            gbsName = "1GBS",
            leaderId = 3L,
            leaderName = "홍길동",
            memberCount = 2,
            members = listOf(member1, member2)
        )
        
        val gbs2 = VillageGbsInfoResponse.GbsInfo(
            gbsId = 2L,
            gbsName = "2GBS",
            leaderId = 4L,
            leaderName = "김리더",
            memberCount = 0,
            members = emptyList()
        )
        
        val response = VillageGbsInfoResponse(
            villageId = villageId,
            villageName = "1마을",
            gbsCount = 2,
            totalMemberCount = 2,
            gbsList = listOf(gbs1, gbs2)
        )
        
        every { organizationService.getVillageGbsInfo(villageId, date) } returns response
        
        // when
        val result = controller.getVillageGbsInfo(villageId, date)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(villageId, responseData?.villageId)
        assertEquals("1마을", responseData?.villageName)
        assertEquals(2, responseData?.gbsCount)
        assertEquals(2, responseData?.totalMemberCount)
        assertEquals(2, responseData?.gbsList?.size)
        assertEquals(1L, responseData?.gbsList?.get(0)?.gbsId)
        assertEquals("1GBS", responseData?.gbsList?.get(0)?.gbsName)
        assertEquals("홍길동", responseData?.gbsList?.get(0)?.leaderName)
        assertEquals(2, responseData?.gbsList?.get(0)?.memberCount)
        assertEquals(2, responseData?.gbsList?.get(0)?.members?.size)
        assertEquals(2L, responseData?.gbsList?.get(1)?.gbsId)
        assertEquals("2GBS", responseData?.gbsList?.get(1)?.gbsName)
    }

    @Test
    fun `마을 내 GBS 출석 데이터 수정`() {
        // given
        val villageId = 1L
        val gbsId = 1L
        val weekStart = LocalDate.now().with(java.time.DayOfWeek.SUNDAY)
        
        val request = AttendanceUpdateRequestDto(
            gbsId = gbsId,
            weekStart = weekStart,
            attendances = listOf(
                AttendanceMemberItemDto(memberId = 1L, worship = WorshipStatus.O, qtCount = 5, ministry = MinistryStatus.A),
                AttendanceMemberItemDto(memberId = 2L, worship = WorshipStatus.X, qtCount = 2, ministry = MinistryStatus.B)
            )
        )
        
        val response = listOf(
            AttendanceResponse(
                id = 1L,
                memberId = 1L,
                memberName = "김조원",
                weekStart = weekStart,
                worship = WorshipStatus.O,
                qtCount = 5,
                ministry = MinistryStatus.A
            ),
            AttendanceResponse(
                id = 2L,
                memberId = 2L,
                memberName = "이조원",
                weekStart = weekStart,
                worship = WorshipStatus.X,
                qtCount = 2,
                ministry = MinistryStatus.B
            )
        )
        
        every { attendanceService.updateVillageGbsAttendance(villageId, request) } returns response
        
        // when
        val result = controller.updateVillageGbsAttendance(villageId, request)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(2, responseData?.items?.size)
        assertEquals(1L, responseData?.items?.get(0)?.id)
        assertEquals(1L, responseData?.items?.get(0)?.memberId)
        assertEquals("김조원", responseData?.items?.get(0)?.memberName)
        assertEquals(WorshipStatus.O, responseData?.items?.get(0)?.worship)
        assertEquals(5, responseData?.items?.get(0)?.qtCount)
        assertEquals(MinistryStatus.A, responseData?.items?.get(0)?.ministry)
        assertEquals(2L, responseData?.items?.get(1)?.id)
        assertEquals(2L, responseData?.items?.get(1)?.memberId)
        assertEquals("이조원", responseData?.items?.get(1)?.memberName)
        assertEquals(WorshipStatus.X, responseData?.items?.get(1)?.worship)
        assertEquals(2, responseData?.items?.get(1)?.qtCount)
        assertEquals(MinistryStatus.B, responseData?.items?.get(1)?.ministry)
    }
} 