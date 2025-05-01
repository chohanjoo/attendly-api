package com.church.attendly.api.controller

import com.church.attendly.api.controller.LeaderDelegationResponse
import com.church.attendly.domain.entity.*
import com.church.attendly.service.DelegationCreateRequest
import com.church.attendly.service.LeaderDelegationService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.time.LocalDate
import java.time.LocalDateTime

class LeaderDelegationControllerTest {
    
    @MockK
    private lateinit var leaderDelegationService: LeaderDelegationService
    
    private lateinit var mockMvc: MockMvc
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        
        objectMapper = Jackson2ObjectMapperBuilder.json()
            .modules(JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
        
        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        
        val controller = LeaderDelegationController(leaderDelegationService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(converter)
            .build()
    }
    
    @Test
    fun `should return active delegations`() {
        // Given
        val today = LocalDate.now()
        val userId = 2L
        
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
        every { delegation.startDate } returns today.minusDays(1)
        every { delegation.endDate } returns today.plusDays(5)
        
        val delegations = listOf(delegation)
        
        // When
        every { leaderDelegationService.findActiveDelegations(userId, today) } returns delegations
        
        // Then
        mockMvc.perform(
            get("/api/delegations/active")
                .param("userId", userId.toString())
                .param("date", today.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].delegatorId").value(1))
            .andExpect(jsonPath("$[0].delegatorName").value("김위임자"))
            .andExpect(jsonPath("$[0].delegateeId").value(2))
            .andExpect(jsonPath("$[0].delegateeName").value("이수임자"))
            .andExpect(jsonPath("$[0].gbsGroupId").value(1))
            .andExpect(jsonPath("$[0].gbsGroupName").value("테스트 소그룹"))
            .andExpect(jsonPath("$[0].startDate").value(today.minusDays(1).toString()))
            .andExpect(jsonPath("$[0].endDate").value(today.plusDays(5).toString()))
    }
    
    @Test
    fun `should use current date when date parameter is not provided`() {
        // Given
        val today = LocalDate.now()
        val userId = 2L
        
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
        every { delegation.startDate } returns today.minusDays(1)
        every { delegation.endDate } returns today.plusDays(5)
        
        val delegations = listOf(delegation)
        
        // When
        every { leaderDelegationService.findActiveDelegations(any(), any()) } returns delegations
        
        // Then
        mockMvc.perform(
            get("/api/delegations/active")
                .param("userId", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].delegatorName").value("김위임자"))
    }
} 