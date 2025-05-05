package com.attendly.service

import com.attendly.domain.entity.*
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class LeaderDelegationServiceTest {
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var gbsGroupRepository: GbsGroupRepository
    private lateinit var leaderDelegationService: LeaderDelegationService
    
    private lateinit var delegator: User
    private lateinit var delegatee: User
    private lateinit var gbsGroup: GbsGroup
    private lateinit var department: Department
    private lateinit var village: Village
    
    @BeforeEach
    fun setup() {
        // 모든 mock 객체를 relaxed=true로 생성
        leaderDelegationRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        gbsGroupRepository = mockk(relaxed = true)
        
        // 실제 객체 생성
        department = Department(
            id = 1L,
            name = "교구"
        )
        
        village = Village(
            id = 2L,
            name = "마을",
            department = department
        )
        
        gbsGroup = GbsGroup(
            id = 3L,
            name = "GBS 그룹",
            village = village,
            termStartDate = LocalDate.now().minusMonths(1),
            termEndDate = LocalDate.now().plusMonths(5)
        )

        delegator = User(
            id = 1L,
            name = "위임자",
            role = Role.LEADER,
            department = department
        )
        
        delegatee = User(
            id = 2L,
            name = "위임받는자",
            role = Role.MEMBER,
            department = department
        )
        
        // 서비스 인스턴스 생성
        leaderDelegationService = LeaderDelegationService(
            leaderDelegationRepository = leaderDelegationRepository,
            userRepository = userRepository,
            gbsGroupRepository = gbsGroupRepository
        )
    }
    
    @Test
    fun `createDelegation should create and save delegation when input is valid`() {
        // Given
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(7)
        
        val request = DelegationCreateRequest(
            delegatorId = delegator.id!!,
            delegateId = delegatee.id!!,
            gbsGroupId = gbsGroup.id!!,
            startDate = startDate,
            endDate = endDate
        )
        
        every { userRepository.findById(delegator.id!!) } returns Optional.of(delegator)
        every { userRepository.findById(delegatee.id!!) } returns Optional.of(delegatee)
        every { gbsGroupRepository.findById(gbsGroup.id!!) } returns Optional.of(gbsGroup)
        every { leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate) } returns null
        
        val savedDelegation = LeaderDelegation(
            id = 1L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = endDate
        )
        
        val delegationSlot = slot<LeaderDelegation>()
        every { leaderDelegationRepository.save(capture(delegationSlot)) } returns savedDelegation
        
        // When
        val result = leaderDelegationService.createDelegation(request)
        
        // Then
        assertEquals(savedDelegation, result)
        
        val capturedDelegation = delegationSlot.captured
        assertEquals(delegator, capturedDelegation.delegator)
        assertEquals(delegatee, capturedDelegation.delegatee)
        assertEquals(gbsGroup, capturedDelegation.gbsGroup)
        assertEquals(startDate, capturedDelegation.startDate)
        assertEquals(endDate, capturedDelegation.endDate)
    }
    
    @Test
    fun `createDelegation should throw IllegalArgumentException when delegator not found`() {
        // Given
        val request = DelegationCreateRequest(
            delegatorId = 999L,
            delegateId = delegatee.id!!,
            gbsGroupId = gbsGroup.id!!,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(7)
        )
        
        every { userRepository.findById(999L) } returns Optional.empty()
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            leaderDelegationService.createDelegation(request)
        }
        
        assertEquals("Delegator not found", exception.message)
    }
    
    @Test
    fun `createDelegation should throw IllegalArgumentException when delegatee not found`() {
        // Given
        val request = DelegationCreateRequest(
            delegatorId = delegator.id!!,
            delegateId = 999L,
            gbsGroupId = gbsGroup.id!!,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(7)
        )
        
        every { userRepository.findById(delegator.id!!) } returns Optional.of(delegator)
        every { userRepository.findById(999L) } returns Optional.empty()
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            leaderDelegationService.createDelegation(request)
        }
        
        assertEquals("Delegate not found", exception.message)
    }
    
    @Test
    fun `createDelegation should throw IllegalArgumentException when gbsGroup not found`() {
        // Given
        val request = DelegationCreateRequest(
            delegatorId = delegator.id!!,
            delegateId = delegatee.id!!,
            gbsGroupId = 999L,
            startDate = LocalDate.now().plusDays(1),
            endDate = LocalDate.now().plusDays(7)
        )
        
        every { userRepository.findById(delegator.id!!) } returns Optional.of(delegator)
        every { userRepository.findById(delegatee.id!!) } returns Optional.of(delegatee)
        every { gbsGroupRepository.findById(999L) } returns Optional.empty()
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            leaderDelegationService.createDelegation(request)
        }
        
        assertEquals("GBS Group not found", exception.message)
    }
    
    @Test
    fun `createDelegation should throw IllegalArgumentException when startDate is not in the future`() {
        // Given
        val pastDate = LocalDate.now().minusDays(1)
        val endDate = LocalDate.now().plusDays(7)
        
        val request = DelegationCreateRequest(
            delegatorId = delegator.id!!,
            delegateId = delegatee.id!!,
            gbsGroupId = gbsGroup.id!!,
            startDate = pastDate,
            endDate = endDate
        )
        
        every { userRepository.findById(delegator.id!!) } returns Optional.of(delegator)
        every { userRepository.findById(delegatee.id!!) } returns Optional.of(delegatee)
        every { gbsGroupRepository.findById(gbsGroup.id!!) } returns Optional.of(gbsGroup)
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            leaderDelegationService.createDelegation(request)
        }
        
        assertEquals("Start date must be in the future", exception.message)
    }
    
    @Test
    fun `createDelegation should throw IllegalArgumentException when there is already an active delegation`() {
        // Given
        val startDate = LocalDate.now().plusDays(1)
        val endDate = LocalDate.now().plusDays(7)
        
        val request = DelegationCreateRequest(
            delegatorId = delegator.id!!,
            delegateId = delegatee.id!!,
            gbsGroupId = gbsGroup.id!!,
            startDate = startDate,
            endDate = endDate
        )
        
        val existingDelegation = LeaderDelegation(
            id = 99L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(5)
        )
        
        every { userRepository.findById(delegator.id!!) } returns Optional.of(delegator)
        every { userRepository.findById(delegatee.id!!) } returns Optional.of(delegatee)
        every { gbsGroupRepository.findById(gbsGroup.id!!) } returns Optional.of(gbsGroup)
        every { leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate) } returns existingDelegation
        
        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            leaderDelegationService.createDelegation(request)
        }
        
        assertEquals("There is already an active delegation for this GBS group", exception.message)
    }
    
    @Test
    fun `findActiveDelegations should return active delegations for the given user and date`() {
        // Given
        val userId = 2L
        val date = LocalDate.now()
        
        val delegation = LeaderDelegation(
            id = 1L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = date.minusDays(1),
            endDate = date.plusDays(5)
        )
        
        every { leaderDelegationRepository.findActiveByDelegateIdAndDate(userId, date) } returns listOf(delegation)
        
        // When
        val result = leaderDelegationService.findActiveDelegations(userId, date)
        
        // Then
        assertEquals(1, result.size)
        assertEquals(delegation, result.first())
        
        verify(exactly = 1) { leaderDelegationRepository.findActiveByDelegateIdAndDate(userId, date) }
    }
}