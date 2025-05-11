package com.attendly.api.controller

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.entity.User
import com.attendly.service.DelegationCreateRequest
import com.attendly.service.LeaderDelegationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate

class LeaderDelegationControllerSecurityTest {
    
    @MockK
    private lateinit var leaderDelegationService: LeaderDelegationService
    
    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        
        val controller = LeaderDelegationController(leaderDelegationService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        
        objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
    }
    
    @Test
    @WithMockUser(roles = ["LEADER"])
    fun `createDelegation should return created delegation`() {
        // Given
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(7)
        
        val request = DelegationCreateRequest(
            delegatorId = 1L,
            delegateId = 2L,
            gbsGroupId = 1L,
            startDate = startDate,
            endDate = endDate
        )
        
        val delegator = mockk<User>()
        every { delegator.id } returns 1L
        every { delegator.name } returns "김위임자"
        
        val delegatee = mockk<User>()
        every { delegatee.id } returns 2L
        every { delegatee.name } returns "이수임자"
        
        val gbsGroup = mockk<GbsGroup>()
        every { gbsGroup.id } returns 1L
        every { gbsGroup.name } returns "테스트 소그룹"
        
        val delegation = mockk<LeaderDelegation>()
        every { delegation.id } returns 1L
        every { delegation.delegator } returns delegator
        every { delegation.delegatee } returns delegatee
        every { delegation.gbsGroup } returns gbsGroup
        every { delegation.startDate } returns startDate
        every { delegation.endDate } returns endDate
        
        every { leaderDelegationService.createDelegation(any()) } returns delegation
        
        // When/Then
        mockMvc.perform(
            post("/api/delegations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(1L))
    }
    
    @Test
    fun `getActiveDelegations should be accessible without authentication`() {
        // Given
        val userId = 1L
        val date = LocalDate.now()
        
        every { leaderDelegationService.findActiveDelegations(userId, date) } returns emptyList()
        
        // When/Then
        mockMvc.perform(
            get("/api/delegations/active")
                .param("userId", userId.toString())
                .param("date", date.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items").isArray)
    }
} 