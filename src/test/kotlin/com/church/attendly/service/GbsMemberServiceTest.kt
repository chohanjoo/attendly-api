package com.church.attendly.service

import com.church.attendly.api.dto.GbsMemberResponse
import com.church.attendly.api.dto.GbsMembersListResponse
import com.church.attendly.api.dto.LeaderGbsResponse
import com.church.attendly.domain.entity.*
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.LeaderDelegationRepository
import com.church.attendly.exception.AccessDeniedException
import com.church.attendly.exception.ResourceNotFoundException
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
        assertThrows(AccessDeniedException::class.java) {
            gbsMemberService.getGbsMembers(gbsId, date, villageLeaderUser)
        }
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
        assertThrows(AccessDeniedException::class.java) {
            gbsMemberService.getGbsMembers(gbsId, date, leaderUser)
        }
    }

    @Test
    fun `getGbsForLeader는 리더가 속한 GBS 정보를 반환한다`() {
        // Given
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = 1L, name = "1마을", department = department)
        val gbsGroup = GbsGroup(
            id = gbsId, 
            name = "GBS1", 
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        val leader = User(
            id = userId,
            name = "리더",
            role = Role.LEADER,
            email = "leader@example.com",
            password = "password",
            department = department
        )
        
        val startDate = LocalDate.of(2023, 1, 1)
        val leaderHistory = GbsLeaderHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = startDate,
            endDate = null
        )

        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) } returns leaderHistory

        // When
        val result = gbsMemberService.getGbsForLeader(userId)

        // Then
        assertEquals(gbsId, result.gbsId)
        assertEquals("GBS1", result.gbsName)
        assertEquals(1L, result.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(userId, result.leaderId)
        assertEquals("리더", result.leaderName)
        assertEquals(startDate, result.startDate)
        
        verify { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) }
    }

    @Test
    fun `getGbsForLeader는 리더가 속한 GBS가 없을 경우 예외를 발생시킨다`() {
        // Given
        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(userId) } returns null

        // When & Then
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            gbsMemberService.getGbsForLeader(userId)
        }
        
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
        
        leaderUser.gbsLeaderHistories.addAll(listOf(history1, history2))
        
        // When
        every { organizationService.getGbsMembers(gbsGroup1.id!!, history1.endDate!!) } returns members1Response
        every { organizationService.getGbsMembers(gbsGroup2.id!!) } returns members2Response
        
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
    fun `getLeaderGbsHistories는 관리자가 다른 리더의 히스토리를 조회할 수 있다`() {
        // Given
        val leaderId = 1L
        val adminId = 99L
        val adminUser = createUser(Role.ADMIN, adminId)
        val leaderUser = createUser(Role.LEADER, leaderId)
        
        val history = GbsLeaderHistory(
            id = 1L,
            gbsGroup = createGbsGroup(10L),
            leader = leaderUser,
            startDate = LocalDate.of(2022, 1, 1),
            endDate = LocalDate.of(2022, 12, 31)
        )
        
        leaderUser.gbsLeaderHistories.add(history)
        
        val membersResponse = createGbsMembersListResponse(history.gbsGroup.id!!)
        
        // When
        every { organizationService.getGbsMembers(history.gbsGroup.id!!, history.endDate!!) } returns membersResponse
        every { userService.findById(leaderId) } returns Optional.of(leaderUser)
        
        val result = gbsMemberService.getLeaderGbsHistories(leaderId, adminUser)
        
        // Then
        assertEquals(1, result.historyCount)
        assertEquals(leaderId, result.leaderId)
    }
    
    @Test
    fun `getLeaderGbsHistories는 일반 리더가 다른 리더의 히스토리를 조회할 수 없다`() {
        // Given
        val leaderId = 1L
        val otherLeaderId = 2L
        val otherLeaderUser = createUser(Role.LEADER, otherLeaderId)
        
        // When & Then
        assertThrows(AccessDeniedException::class.java) {
            gbsMemberService.getLeaderGbsHistories(leaderId, otherLeaderUser)
        }
    }

    // 테스트 헬퍼 메소드
    private fun createUser(role: Role, userId: Long = 1L): User {
        val department = Department(id = 1L, name = "대학부")
        return User(
            id = userId,
            name = "User Name",
            email = "user@example.com",
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            role = role,
            department = department
        )
    }

    private fun createVillageLeaderUser(villageId: Long): User {
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = villageId, name = "Village Name", department = department)
        val user = createUser(Role.VILLAGE_LEADER)
        val villageLeader = VillageLeader(
            user = user, 
            village = village,
            startDate = LocalDate.now()
        )
        
        // VillageLeader 설정
        val userWithVillageLeader = User(
            id = user.id,
            name = user.name,
            email = user.email,
            password = user.password,
            birthDate = user.birthDate,
            role = Role.VILLAGE_LEADER,
            department = department
        )
        
        // 리플렉션을 통해 villageLeader 필드에 접근하여 값을 설정 (테스트용)
        val field = User::class.java.getDeclaredField("villageLeader")
        field.isAccessible = true
        field.set(userWithVillageLeader, villageLeader)
        
        return userWithVillageLeader
    }

    private fun createGbsGroup(villageId: Long): GbsGroup {
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = villageId, name = "Village Name", department = department)
        return GbsGroup(
            id = 1L, 
            name = "GBS Group", 
            village = village, 
            termStartDate = LocalDate.of(2023, 1, 1), 
            termEndDate = LocalDate.of(2023, 12, 31)
        )
    }

    private fun createGbsMembersListResponse(gbsId: Long): GbsMembersListResponse {
        return GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS Group",
            memberCount = 2,
            members = listOf(
                GbsMemberResponse(
                    id = 1L, 
                    name = "Member 1", 
                    email = "member1@example.com",
                    birthDate = LocalDate.of(1995, 1, 1),
                    joinDate = LocalDate.of(2023, 1, 1)
                ),
                GbsMemberResponse(
                    id = 2L, 
                    name = "Member 2", 
                    email = "member2@example.com",
                    birthDate = LocalDate.of(1996, 1, 1),
                    joinDate = LocalDate.of(2023, 1, 1)
                )
            )
        )
    }

    private fun mockGbsLeaderHistory(): GbsLeaderHistory {
        val user = createUser(Role.LEADER)
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = 1L, name = "Village", department = department)
        val gbsGroup = GbsGroup(
            id = 1L, 
            name = "GBS Group", 
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1), 
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        
        return GbsLeaderHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            leader = user,
            startDate = LocalDate.of(2023, 1, 1)
        )
    }

    private fun mockLeaderDelegation(): LeaderDelegation {
        val delegator = createUser(Role.LEADER, 1L)
        val delegatee = createUser(Role.LEADER, 2L)
        val department = Department(id = 1L, name = "대학부")
        val village = Village(id = 1L, name = "Village", department = department)
        val gbsGroup = GbsGroup(
            id = 1L, 
            name = "GBS Group", 
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1), 
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        
        return LeaderDelegation(
            id = 1L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = LocalDate.of(2023, 5, 1)
        )
    }
} 