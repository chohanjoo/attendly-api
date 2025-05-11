package com.attendly.api.controller

import com.attendly.api.dto.LeaderDelegationResponse
import com.attendly.domain.entity.*
import com.attendly.service.DelegationCreateRequest
import com.attendly.service.LeaderDelegationService
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
class LeaderDelegationControllerTest {
    
    private lateinit var controller: LeaderDelegationController
    private lateinit var leaderDelegationService: LeaderDelegationService
    
    @BeforeEach
    fun setup() {
        leaderDelegationService = mockk()
        controller = LeaderDelegationController(leaderDelegationService)
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
        
        val result = controller.getActiveDelegations(userId, today)
        
        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(1, responseData?.items?.size)
        
        val delegationResponse = responseData?.items?.get(0)
        assertEquals(1L, delegationResponse?.id)
        assertEquals(1L, delegationResponse?.delegatorId)
        assertEquals("김위임자", delegationResponse?.delegatorName)
        assertEquals(2L, delegationResponse?.delegateeId)
        assertEquals("이수임자", delegationResponse?.delegateeName)
        assertEquals(1L, delegationResponse?.gbsGroupId)
        assertEquals("테스트 소그룹", delegationResponse?.gbsGroupName)
        assertEquals(today.minusDays(1), delegationResponse?.startDate)
        assertEquals(today.plusDays(5), delegationResponse?.endDate)
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
        every { leaderDelegationService.findActiveDelegations(userId, any()) } returns delegations
        
        val result = controller.getActiveDelegations(userId, null)
        
        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(1, responseData?.items?.size)
        
        val delegationResponse = responseData?.items?.get(0)
        assertEquals(1L, delegationResponse?.id)
        assertEquals("김위임자", delegationResponse?.delegatorName)
    }
    
    @Test
    fun `should create delegation and return created response`() {
        // Given
        val request = DelegationCreateRequest(
            delegatorId = 1L,
            delegateId = 2L,
            gbsGroupId = 1L,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7)
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
        
        val createdDelegation = mockk<LeaderDelegation>()
        every { createdDelegation.id } returns 1L
        every { createdDelegation.delegator } returns delegator
        every { createdDelegation.delegatee } returns delegatee
        every { createdDelegation.gbsGroup } returns gbsGroup
        every { createdDelegation.startDate } returns request.startDate
        every { createdDelegation.endDate } returns request.endDate
        
        // When
        every { leaderDelegationService.createDelegation(request) } returns createdDelegation
        
        val result = controller.createDelegation(request)
        
        // Then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val delegationResponse = result.body?.data
        assertEquals(1L, delegationResponse?.id)
        assertEquals(1L, delegationResponse?.delegatorId)
        assertEquals("김위임자", delegationResponse?.delegatorName)
        assertEquals(2L, delegationResponse?.delegateeId)
        assertEquals("이수임자", delegationResponse?.delegateeName)
        assertEquals(1L, delegationResponse?.gbsGroupId)
        assertEquals("테스트 소그룹", delegationResponse?.gbsGroupName)
        assertEquals(request.startDate, delegationResponse?.startDate)
        assertEquals(request.endDate, delegationResponse?.endDate)
    }
} 