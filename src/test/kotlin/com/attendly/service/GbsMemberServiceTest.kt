package com.attendly.service

import com.attendly.api.dto.GbsAssignment
import com.attendly.api.dto.GbsAssignmentSaveRequest
import com.attendly.api.dto.GbsAssignmentSaveResponse
import com.attendly.api.dto.GbsMemberSearchDto
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.LeaderGbsResponse
import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.LeaderCandidateResponse
import com.attendly.api.dto.LeaderCandidate
import com.attendly.api.dto.LeaderGbsHistoryRequestDto
import com.attendly.domain.entity.*
import com.attendly.domain.model.UserFilterDto
import com.attendly.domain.repository.*
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class GbsMemberServiceTest {

    private lateinit var organizationService: OrganizationService
    private lateinit var gbsPermissionValidator: GbsPermissionValidator
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository
    private lateinit var userService: UserService
    private lateinit var villageRepository: VillageRepository
    private lateinit var gbsGroupRepository: GbsGroupRepository
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var gbsMemberService: GbsMemberService

    private val gbsId = 100L
    private val userId = 1L

    @BeforeEach
    fun setUp() {
        organizationService = mockk()
        gbsPermissionValidator = mockk()
        gbsLeaderHistoryRepository = mockk()
        leaderDelegationRepository = mockk()
        userService = mockk()
        villageRepository = mockk()
        gbsGroupRepository = mockk()
        gbsMemberHistoryRepository = mockk()
        userRepository = mockk()

        gbsMemberService = GbsMemberService(
            organizationService,
            gbsPermissionValidator,
            gbsLeaderHistoryRepository,
            leaderDelegationRepository,
            userService,
            villageRepository,
            gbsGroupRepository,
            gbsMemberHistoryRepository,
            userRepository
        )
    }

    @Test
    fun `관리자는 모든 GBS 멤버를 조회할 수 있다`() {
        // given
        val date = LocalDate.now()
        val adminUser = createUser(Role.ADMIN)
        val expectedResponse = createGbsMembersListResponse(gbsId)
        val searchDto = GbsMemberSearchDto(gbsId, date, adminUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, adminUser) } returns Unit
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(searchDto)

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
        val searchDto = GbsMemberSearchDto(gbsId, date, ministerUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, ministerUser) } returns Unit
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(searchDto)

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
        val searchDto = GbsMemberSearchDto(gbsId, date, villageLeaderUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, villageLeaderUser) } returns Unit
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(searchDto)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `마을장은 다른 마을의 GBS 멤버를 조회할 수 없다`() {
        // given
        val villageId = 10L
        val date = LocalDate.now()
        val villageLeaderUser = createVillageLeaderUser(villageId)
        val searchDto = GbsMemberSearchDto(gbsId, date, villageLeaderUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, villageLeaderUser) } throws AttendlyApiException(ErrorMessage.ACCESS_DENIED_GBS)

        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getGbsMembers(searchDto)
        }

        assertEquals(ErrorMessage.ACCESS_DENIED_GBS.code, exception.errorMessage.code)
    }

    @Test
    fun `리더는 자신의 GBS 멤버를 조회할 수 있다`() {
        // given
        val date = LocalDate.now()
        val leaderUser = createUser(Role.LEADER, userId)
        val expectedResponse = createGbsMembersListResponse(gbsId)
        val searchDto = GbsMemberSearchDto(gbsId, date, leaderUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, leaderUser) } returns Unit
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(searchDto)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `위임받은 리더는 해당 GBS 멤버를 조회할 수 있다`() {
        // given
        val delegateeId = 200L
        val date = LocalDate.now()
        val delegateeUser = createUser(Role.LEADER, delegateeId)
        val expectedResponse = createGbsMembersListResponse(gbsId)
        val searchDto = GbsMemberSearchDto(gbsId, date, delegateeUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, delegateeUser) } returns Unit
        every { organizationService.getGbsMembers(gbsId, date) } returns expectedResponse

        // when
        val result = gbsMemberService.getGbsMembers(searchDto)

        // then
        assertEquals(expectedResponse, result)
        verify(exactly = 1) { organizationService.getGbsMembers(gbsId, date) }
    }

    @Test
    fun `권한이 없는 리더는 다른 GBS 멤버를 조회할 수 없다`() {
        // given
        val date = LocalDate.now()
        val leaderUser = createUser(Role.LEADER, userId)
        val searchDto = GbsMemberSearchDto(gbsId, date, leaderUser)

        every { gbsPermissionValidator.validateGbsAccessPermission(gbsId, date, leaderUser) } throws AttendlyApiException(ErrorMessage.ACCESS_DENIED_GBS)

        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getGbsMembers(searchDto)
        }

        assertEquals(ErrorMessage.ACCESS_DENIED_GBS.code, exception.errorMessage.code)
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
            termStartDate = startDate,
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

        assertEquals(ErrorMessage.NO_CURRENT_GBS_FOR_LEADER.code, exception.errorMessage.code)
    }

    @Test
    fun `리더 후보 목록을 성공적으로 조회한다`() {
        // given
        val villageId = 1L
        val villageName = "테스트 마을"
        val village = Village(id = villageId, name = villageName, department = mockk())
        val department = mockk<Department>()
        val user1 = User(id = 1L, name = "박지성", email = "park@example.com", role = Role.LEADER, department = department)
        val user2 = User(id = 2L, name = "손흥민", email = "son@example.com", role = Role.LEADER, department = department)
        val userFilterDto = UserFilterDto(villageId = villageId, roles = listOf(Role.LEADER))

        val leaderHistoryUser1 = listOf(
            mockk<GbsLeaderHistory>(),
            mockk<GbsLeaderHistory>()
        )

        val leaderHistoryUser2 = listOf(
            mockk<GbsLeaderHistory>()
        )

        every { villageRepository.findById(villageId) } returns java.util.Optional.of(village)
        every { userRepository.findByFilters(userFilterDto) } returns listOf(user1, user2)
        every { gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(1L) } returns leaderHistoryUser1
        every { gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(2L) } returns leaderHistoryUser2
        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(1L) } returns mockk()
        every { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(2L) } returns mockk()

        // when
        val result = gbsMemberService.getLeaderCandidates(villageId)

        // then
        assertEquals(villageId, result.villageId)
        assertEquals(villageName, result.villageName)
        assertEquals(2, result.candidates.size)

        val firstCandidate = result.candidates[0]
        assertEquals(1L, firstCandidate.id)
        assertEquals("박지성", firstCandidate.name)
        assertEquals("park@example.com", firstCandidate.email)
        assertEquals(true, firstCandidate.isLeader)
        assertEquals(2, firstCandidate.previousGbsCount)

        val secondCandidate = result.candidates[1]
        assertEquals(2L, secondCandidate.id)
        assertEquals("손흥민", secondCandidate.name)
        assertEquals("son@example.com", secondCandidate.email)
        assertEquals(true, secondCandidate.isLeader)
        assertEquals(1, secondCandidate.previousGbsCount)

        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { userRepository.findByFilters(userFilterDto) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(1L) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findByLeaderIdOrderByStartDateDesc(2L) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(1L) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findByLeaderIdAndEndDateIsNull(2L) }
    }

    @Test
    fun `존재하지 않는 마을의 리더 후보 목록 조회시 예외가 발생한다`() {
        // given
        val villageId = 999L

        every { villageRepository.findById(villageId) } returns java.util.Optional.empty()

        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.getLeaderCandidates(villageId)
        }

        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.code, exception.errorMessage.code)
        verify(exactly = 1) { villageRepository.findById(villageId) }
    }

    // Private helper methods
    private fun createUser(role: Role, id: Long = 1L): User {
        val user = mockk<User>()
        every { user.id } returns id
        every { user.role } returns role
        every { user.name } returns "테스트 사용자"
        every { user.gbsLeaderHistories } returns mutableListOf()
        every { user.villageLeader } returns null
        every { user.department } returns mockk<Department>()
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
                    joinDate = LocalDate.now(),
                    phoneNumber = "010-1234-5678"
                ),
                GbsMemberResponse(
                    id = 2L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = LocalDate.of(2000, 2, 2),
                    joinDate = LocalDate.now(),
                    phoneNumber = "010-1234-5678"
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