package com.attendly.service

import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.LeaderGbsResponse
import com.attendly.domain.entity.*
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockKExtension::class)
class GbsMemberServiceTest {

    @MockK
    private lateinit var organizationService: OrganizationService

    @MockK
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @MockK
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository
    
    @MockK
    private lateinit var userService: UserService

    @InjectMockKs
    private lateinit var gbsMemberService: GbsMemberService

    private val gbsId = 1L
    private val userId = 1L
    private val targetDate = LocalDate.now()

    @BeforeEach
    fun setup() {
        organizationService = mockk()
        gbsLeaderHistoryRepository = mockk()
        leaderDelegationRepository = mockk()
        userService = mockk()

        gbsMemberService = GbsMemberService(
            organizationService,
            gbsLeaderHistoryRepository,
            leaderDelegationRepository,
            userService
        )
    }

    @Test
    fun `관리자는 모든 GBS 멤버를 조회할 수 있다`() {
        // given
        val date = LocalDate.now()
        val adminUser = createUser(Role.ADMIN)
        val expectedResponse = createGbsMembersListResponse(gbsId)

        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(gbsId, date, adminUser)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `교역자는 모든 GBS 멤버를 조회할 수 있다`() {
        // given
        val date = LocalDate.now()
        val ministerUser = createUser(Role.MINISTER)
        val expectedResponse = createGbsMembersListResponse(gbsId)

        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(gbsId, date, ministerUser)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `마을장은 자신의 마을에 속한 GBS의 멤버를 조회할 수 있다`() {
        // given
        val villageId = 10L
        val date = LocalDate.now()
        val villageLeaderUser = createVillageLeaderUser(villageId)
        val expectedResponse = createGbsMembersListResponse(gbsId)
        val gbsGroup = createGbsGroup(villageId)

        every { organizationService.getGbsGroupById(gbsId) } returns gbsGroup
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(gbsId, date, villageLeaderUser)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsGroupById(gbsId) }
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `마을장은 다른 마을의 GBS 멤버를 조회할 수 없다`() {
        // given
        val villageId = 10L
        val otherVillageId = 20L
        val date = LocalDate.now()
        val villageLeaderUser = createVillageLeaderUser(villageId)
        val gbsGroup = createGbsGroup(otherVillageId) // 다른 마을의 GBS

        every { organizationService.getGbsGroupById(gbsId) } returns gbsGroup

        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getGbsMembers(gbsId, date, villageLeaderUser)
        }
        
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `리더는 자신의 GBS 멤버를 조회할 수 있다`() {
        // given
        val date = LocalDate.now()
        val leaderUser = createUser(Role.LEADER, userId)
        val expectedResponse = createGbsMembersListResponse(gbsId)

        every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, userId) } returns mockGbsLeaderHistory()
        every { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, userId, date) } returns null
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(gbsId, date, leaderUser)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, userId) }
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `위임받은 리더는 해당 GBS 멤버를 조회할 수 있다`() {
        // given
        val delegateeId = 200L
        val date = LocalDate.now()
        val delegateeUser = createUser(Role.LEADER, delegateeId)
        val expectedResponse = createGbsMembersListResponse(gbsId)

        every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, delegateeId) } returns null
        every { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, delegateeId, date) } returns mockLeaderDelegation()
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(gbsId, date, delegateeUser)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, delegateeId, date) }
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `권한이 없는 리더는 다른 GBS 멤버를 조회할 수 없다`() {
        // given
        val date = LocalDate.now()
        val leaderUser = createUser(Role.LEADER, userId)

        every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, userId) } returns null
        every { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, userId, date) } returns null

        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getGbsMembers(gbsId, date, leaderUser)
        }
        
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `getGbsForLeader는 리더가 속한 GBS 정보를 반환한다`() {
        // Given
        val gbsId = 1L
        val villageId = 10L
        val villageName = "1마을"
        val gbsName = "GBS1"
        val leaderName = "리더1"
        val startDate = LocalDate.of(2023, 1, 1)
        
        val leader = createUser(Role.LEADER, userId)
        every { leader.name } returns leaderName
        
        val village = Village(id = villageId, name = villageName, department = mockk())
        val gbsGroup = GbsGroup(
            id = gbsId,
            name = gbsName,
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = startDate
        )
        
        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) } returns leaderHistory
        
        // When
        val result = gbsMemberService.getGbsForLeader(userId)
        
        // Then
        assertEquals(gbsId, result.gbsId)
        assertEquals(gbsName, result.gbsName)
        assertEquals(villageId, result.villageId)
        assertEquals(villageName, result.villageName)
        assertEquals(userId, result.leaderId)
        assertEquals(leaderName, result.leaderName)
        assertEquals(startDate, result.startDate)
    }

    @Test
    fun `getGbsForLeader는 리더가 속한 GBS가 없을 경우 예외를 발생시킨다`() {
        // Given
        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) } returns null

        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getGbsForLeader(userId)
        }
        
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.errorCode)
        assertEquals("현재 담당하는 GBS가 없습니다", exception.message)
        verify { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) }
    }

    @Test
    fun `getLeaderGbsHistories는 리더의 GBS 히스토리 리스트를 반환한다`() {
        // Given
        val leaderId = 1L
        val leaderUser = createUser(Role.LEADER, leaderId)
        
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = 10L, name = "1마을", department = department)

        // 첫 번째 GBS
        val gbsGroup1 = GbsGroup(
            id = 1L,
            name = "GBS1", 
            village = village,
            termStartDate = LocalDate.of(2022, 1, 1),
            termEndDate = LocalDate.of(2022, 12, 31)
        )
        
        // 두 번째 GBS (현재 활성화된 GBS)
        val gbsGroup2 = GbsGroup(
            id = 2L,
            name = "GBS2", 
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        
        val history1 = GbsLeaderHistory(
            id = 1L,
            gbsGroup = gbsGroup1,
            leader = leaderUser,
            startDate = LocalDate.of(2022, 1, 1),
            endDate = LocalDate.of(2022, 12, 31)
        )
        
        val history2 = GbsLeaderHistory(
            id = 2L,
            gbsGroup = gbsGroup2,
            leader = leaderUser,
            startDate = LocalDate.of(2023, 1, 1),
            endDate = null
        )
        
        val members1Response = createGbsMembersListResponse(gbsGroup1.id!!)
        val members2Response = createGbsMembersListResponse(gbsGroup2.id!!)
        
        every { leaderUser.gbsLeaderHistories }.returns(mutableListOf(history1, history2))
        
        // When
        every { organizationService.getGbsMembers(gbsGroup1.id!!, history1.endDate!!) } returns members1Response
        every { organizationService.getGbsMembers(gbsGroup2.id!!) } returns members2Response
        every { gbsLeaderHistoryRepository.findByLeaderIdWithDetailsOrderByStartDateDesc(leaderId) } returns listOf(history2, history1)
        
        val result = gbsMemberService.getLeaderGbsHistories(leaderId, leaderUser)
        
        // Then
        assertEquals(2, result.historyCount)
        assertEquals(leaderId, result.leaderId)
        assertEquals(leaderUser.name, result.leaderName)
        
        // 첫 번째 히스토리 검증 (최신순 정렬이므로 인덱스 0은 더 최근 히스토리)
        assertEquals(history2.id, result.histories[0].historyId)
        assertEquals(gbsGroup2.id, result.histories[0].gbsId)
        assertEquals(gbsGroup2.name, result.histories[0].gbsName)
        assertEquals(village.id, result.histories[0].villageId)
        assertEquals(village.name, result.histories[0].villageName)
        assertEquals(history2.startDate, result.histories[0].startDate)
        assertEquals(history2.endDate, result.histories[0].endDate)
        assertEquals(true, result.histories[0].isActive)
        assertEquals(members2Response.members, result.histories[0].members)
        
        // 두 번째 히스토리 검증
        assertEquals(history1.id, result.histories[1].historyId)
        assertEquals(gbsGroup1.id, result.histories[1].gbsId)
        assertEquals(gbsGroup1.name, result.histories[1].gbsName)
        assertEquals(village.id, result.histories[1].villageId)
        assertEquals(village.name, result.histories[1].villageName)
        assertEquals(history1.startDate, result.histories[1].startDate)
        assertEquals(history1.endDate, result.histories[1].endDate)
        assertEquals(false, result.histories[1].isActive)
        assertEquals(members1Response.members, result.histories[1].members)
    }

    @Test
    fun `getLeaderGbsHistories는 일반 리더가 다른 리더의 히스토리를 조회할 수 없다`() {
        // Given
        val leaderId = 100L
        val currentUserId = 200L
        val currentUser = createUser(Role.LEADER, currentUserId)

        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getLeaderGbsHistories(leaderId, currentUser)
        }
        
        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    // Private helper methods
    private fun createUser(role: Role, id: Long = 1L): User {
        val user = mockk<User>()
        every { user.id } returns id
        every { user.role } returns role
        every { user.name } returns "테스트 사용자"
        every { user.gbsLeaderHistories } returns mutableListOf()
        every { user.villageLeader } returns null
        return user
    }

    private fun createVillageLeaderUser(villageId: Long): User {
        val user = createUser(Role.VILLAGE_LEADER)
        val village = Village(id = villageId, name = "테스트 마을", department = mockk())
        val villageLeader = VillageLeader(
            user = user, 
            village = village, 
            startDate = LocalDate.now().minusMonths(1)
        )
        every { user.villageLeader } returns villageLeader
        return user
    }

    private fun createGbsGroup(villageId: Long): GbsGroup {
        val village = Village(id = villageId, name = "테스트 마을", department = mockk())
        return GbsGroup(
            id = gbsId,
            name = "테스트 GBS",
            village = village,
            termStartDate = LocalDate.now().minusMonths(6),
            termEndDate = LocalDate.now().plusMonths(6)
        )
    }

    private fun createGbsMembersListResponse(gbsId: Long): GbsMembersListResponse {
        return GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "테스트 GBS",
            memberCount = 2,
            members = listOf(
                GbsMemberResponse(
                    id = 1L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = LocalDate.of(2000, 1, 1),
                    joinDate = LocalDate.now()
                ),
                GbsMemberResponse(
                    id = 2L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = LocalDate.of(2000, 2, 2),
                    joinDate = LocalDate.now()
                )
            )
        )
    }

    private fun mockGbsLeaderHistory(): GbsLeaderHistory {
        return mockk<GbsLeaderHistory>()
    }

    private fun mockLeaderDelegation(): LeaderDelegation {
        return mockk<LeaderDelegation>()
    }
} 