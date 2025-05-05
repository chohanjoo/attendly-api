package com.attendly.api.controller

import com.attendly.domain.entity.LeaderDelegation
import com.attendly.service.DelegationCreateRequest
import com.attendly.service.LeaderDelegationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
        
        val delegation = mockk<LeaderDelegation>(relaxed = true) {
            every { id } returns 1L
        }
        
        every { leaderDelegationService.createDelegation(any()) } returns delegation
        
        // When/Then
        mockMvc.perform(
            post("/api/delegations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
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
    }
} 