package com.attendly.api.controller

import com.attendly.api.dto.VillageLeaderAssignRequest
import com.attendly.api.dto.VillageLeaderResponse
import com.attendly.security.JwtTokenProvider
import com.attendly.service.AdminVillageLeaderService
import com.fasterxml.jackson.databind.ObjectMapper
import com.attendly.security.TestSecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.context.annotation.Import
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WebMvcTest(AdminVillageLeaderController::class)
@Import(TestSecurityConfig::class)
class AdminVillageLeaderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var adminVillageLeaderService: AdminVillageLeaderService
    
    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    @MockkBean
    private lateinit var userDetailsService: UserDetailsService
    
    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `마을장 등록 성공`() {
        // given
        val request = VillageLeaderAssignRequest(
            userId = 1L,
            villageId = 1L,
            startDate = LocalDate.now()
        )

        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now(),
            endDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.assignVillageLeader(any()) } returns response

        // when & then
        mockMvc.perform(
            post("/api/admin/village-leader")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.villageId").value(1))
            .andExpect(jsonPath("$.villageName").value("마을1"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `마을장 조회 성공`() {
        // given
        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now(),
            endDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.getVillageLeader(1L) } returns response

        // when & then
        mockMvc.perform(
            get("/api/admin/village-leader/1")
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.userName").value("홍길동"))
            .andExpect(jsonPath("$.villageId").value(1))
            .andExpect(jsonPath("$.villageName").value("마을1"))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `마을장이 없는 경우 404 반환`() {
        // given
        every { adminVillageLeaderService.getVillageLeader(1L) } returns null

        // when & then
        mockMvc.perform(
            get("/api/admin/village-leader/1")
                .with(csrf())
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `마을장 종료 성공`() {
        // given
        val endDate = LocalDate.now()
        val dateString = endDate.format(DateTimeFormatter.ISO_DATE)

        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now().minusDays(30),
            endDate = endDate,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.terminateVillageLeader(1L, endDate) } returns response

        // when & then
        mockMvc.perform(
            delete("/api/admin/village-leader/1")
                .param("endDate", dateString)
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.endDate").exists())
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `권한 없는 사용자의 마을장 등록 시도`() {
        // given
        val request = VillageLeaderAssignRequest(
            userId = 1L,
            villageId = 1L,
            startDate = LocalDate.now()
        )

        // when & then
        mockMvc.perform(
            post("/api/admin/village-leader")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }
} 