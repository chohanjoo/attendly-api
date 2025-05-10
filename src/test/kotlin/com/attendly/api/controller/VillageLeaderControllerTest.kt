package com.attendly.api.controller

import com.attendly.api.dto.AttendanceMemberItemDto
import com.attendly.api.dto.AttendanceResponse
import com.attendly.api.dto.AttendanceUpdateRequestDto
import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.VillageGbsInfoResponse
import com.attendly.domain.entity.MinistryStatus
import com.attendly.domain.entity.WorshipStatus
import com.attendly.security.JwtTokenProvider
import com.attendly.service.AttendanceService
import com.attendly.service.OrganizationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class VillageLeaderControllerTest {

    @MockK
    private lateinit var organizationService: OrganizationService

    @MockK
    private lateinit var attendanceService: AttendanceService

    @MockK
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @InjectMockKs
    private lateinit var villageLeaderController: VillageLeaderController

    private lateinit var objectMapper: ObjectMapper

    private fun setupMockMvc(): MockMvc {
        return MockMvcBuilders.standaloneSetup(villageLeaderController).build()
    }

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    }

    @Test
    @WithMockUser(roles = ["VILLAGE_LEADER"])
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
        
        // when & then
        val mockMvc = setupMockMvc()
        mockMvc.perform(
            get("/api/village-leader/{villageId}/gbs", villageId)
                .param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.villageId").value(villageId))
            .andExpect(jsonPath("$.villageName").value("1마을"))
            .andExpect(jsonPath("$.gbsCount").value(2))
            .andExpect(jsonPath("$.totalMemberCount").value(2))
            .andExpect(jsonPath("$.gbsList.length()").value(2))
            .andExpect(jsonPath("$.gbsList[0].gbsId").value(1))
            .andExpect(jsonPath("$.gbsList[0].gbsName").value("1GBS"))
            .andExpect(jsonPath("$.gbsList[0].leaderName").value("홍길동"))
            .andExpect(jsonPath("$.gbsList[0].memberCount").value(2))
            .andExpect(jsonPath("$.gbsList[0].members.length()").value(2))
            .andExpect(jsonPath("$.gbsList[1].gbsId").value(2))
            .andExpect(jsonPath("$.gbsList[1].gbsName").value("2GBS"))
    }

    @Test
    @WithMockUser(roles = ["VILLAGE_LEADER"])
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
        
        // when & then
        val mockMvc = setupMockMvc()
        mockMvc.perform(
            post("/api/village-leader/{villageId}/attendance", villageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].memberId").value(1))
            .andExpect(jsonPath("$[0].memberName").value("김조원"))
            .andExpect(jsonPath("$[0].worship").value("O"))
            .andExpect(jsonPath("$[0].qtCount").value(5))
            .andExpect(jsonPath("$[0].ministry").value("A"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].memberId").value(2))
            .andExpect(jsonPath("$[1].memberName").value("이조원"))
            .andExpect(jsonPath("$[1].worship").value("X"))
            .andExpect(jsonPath("$[1].qtCount").value(2))
            .andExpect(jsonPath("$[1].ministry").value("B"))
    }
} 