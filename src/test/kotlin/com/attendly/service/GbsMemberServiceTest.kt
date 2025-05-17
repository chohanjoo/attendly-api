package com.attendly.service

import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.GbsAssignmentSaveRequest
import com.attendly.api.dto.GbsAssignment
import com.attendly.domain.entity.*
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
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

    @MockK
    private lateinit var villageRepository: VillageRepository

    @MockK
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @MockK
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @MockK
    private lateinit var userRepository: UserRepository

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
        villageRepository = mockk()
        gbsGroupRepository = mockk()
        gbsMemberHistoryRepository = mockk()
        userRepository = mockk()

        gbsMemberService = GbsMemberService(
            organizationService,
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
        
        assertEquals(ErrorMessage.ACCESS_DENIED_GBS.code, exception.errorMessage.code)
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
        assertEquals(ErrorMessage.NO_CURRENT_GBS_FOR_LEADER.message, exception.message)
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
        
        assertEquals(ErrorMessage.ACCESS_DENIED_LEADER_HISTORY.code, exception.errorMessage.code)
    }

    @Test
    fun `saveGbsAssignment는 유효한 요청으로 GBS 배치 정보를 저장한다`() {
        // Given
        val villageId = 1L
        val gbsId1 = 101L
        val gbsId2 = 102L
        val leaderId1 = 201L
        val leaderId2 = 202L
        val memberId1 = 301L
        val memberId2 = 302L
        val memberId3 = 303L
        
        val startDate = LocalDate.of(2023, 6, 1)
        val endDate = LocalDate.of(2023, 12, 31)
        
        val village = Village(id = villageId, name = "1마을", department = mockk())
        val gbsGroup1 = GbsGroup(id = gbsId1, name = "GBS1", village = village, termStartDate = startDate, termEndDate = endDate)
        val gbsGroup2 = GbsGroup(id = gbsId2, name = "GBS2", village = village, termStartDate = startDate, termEndDate = endDate)
        
        val department = mockk<Department>()
        val leader1 = User(id = leaderId1, name = "리더1", role = Role.LEADER, department = department)
        val leader2 = User(id = leaderId2, name = "리더2", role = Role.LEADER, department = department)
        val member1 = User(id = memberId1, name = "멤버1", role = Role.MEMBER, department = department)
        val member2 = User(id = memberId2, name = "멤버2", role = Role.MEMBER, department = department)
        val member3 = User(id = memberId3, name = "멤버3", role = Role.MEMBER, department = department)
        
        // 기존 리더 히스토리 - 변경이 필요한 경우
        val previousLeader = mockk<User>(relaxed = true)
        every { previousLeader.id } returns 999L
        
        val existingLeaderHistory1 = mockk<GbsLeaderHistory>(relaxed = true)
        every { existingLeaderHistory1.id } returns 1001L
        every { existingLeaderHistory1.gbsGroup } returns gbsGroup1
        every { existingLeaderHistory1.leader } returns previousLeader
        every { existingLeaderHistory1.startDate } returns LocalDate.of(2023, 1, 1)
        every { existingLeaderHistory1.endDate } returns null
        
        // 요청 객체 생성
        val request = GbsAssignmentSaveRequest(
            startDate = startDate,
            endDate = endDate,
            assignments = listOf(
                GbsAssignment(gbsId = gbsId1, leaderId = leaderId1, memberIds = listOf(memberId1, memberId2)),
                GbsAssignment(gbsId = gbsId2, leaderId = leaderId2, memberIds = listOf(memberId3))
            )
        )
        
        // Mock 설정
        every { villageRepository.findById(villageId) } returns java.util.Optional.of(village)
        
        every { gbsGroupRepository.findById(gbsId1) } returns java.util.Optional.of(gbsGroup1)
        every { gbsGroupRepository.findById(gbsId2) } returns java.util.Optional.of(gbsGroup2)
        
        every { userRepository.findById(leaderId1) } returns java.util.Optional.of(leader1)
        every { userRepository.findById(leaderId2) } returns java.util.Optional.of(leader2)
        every { userRepository.findById(memberId1) } returns java.util.Optional.of(member1)
        every { userRepository.findById(memberId2) } returns java.util.Optional.of(member2)
        every { userRepository.findById(memberId3) } returns java.util.Optional.of(member3)
        
        // GBS1의 기존 리더가 있고 변경 필요
        every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbsId1, startDate) } returns existingLeaderHistory1
        // GBS2는 기존 리더가 없음
        every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbsId2, startDate) } returns null
        
        // GBS1의 기존 멤버는 없음
        every { gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbsId1, startDate) } returns emptyList()
        // GBS2의 기존 멤버는 없음
        every { gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbsId2, startDate) } returns emptyList()
        
        every { gbsLeaderHistoryRepository.save(any()) } returnsArgument 0
        every { gbsMemberHistoryRepository.save(any()) } returnsArgument 0
        
        // When
        val result = gbsMemberService.saveGbsAssignment(villageId, request)
        
        // Then
        assertEquals(villageId, result.villageId)
        assertEquals(2, result.assignmentCount)
        assertEquals(5, result.memberCount) // 리더 2명 + 멤버 3명
        assertEquals("GBS 배치가 성공적으로 저장되었습니다.", result.message)
        
        // 기존 리더 종료 확인
        verify(exactly = 1) { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbsId1, startDate) }
        verify(exactly = 1) { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsId(gbsId2, startDate) }
        
        // 리더 저장 확인 - 총 3회 호출 (기존 리더 종료 1회, 새 리더 등록 2회)
        verify(atLeast = 3) { gbsLeaderHistoryRepository.save(any()) }
        
        // 멤버 등록 확인
        verify(exactly = 1) { gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbsId1, startDate) }
        verify(exactly = 1) { gbsMemberHistoryRepository.findCurrentMembersByGbsId(gbsId2, startDate) }
        verify(exactly = 3) { gbsMemberHistoryRepository.save(any()) }
    }
    
    @Test
    fun `saveGbsAssignment는 마을에 속하지 않은 GBS에 대한 요청을 거부한다`() {
        // Given
        val villageId = 1L
        val otherVillageId = 2L
        val gbsId = 101L
        val leaderId = 201L
        
        val startDate = LocalDate.of(2023, 6, 1)
        val endDate = LocalDate.of(2023, 12, 31)
        
        val village = Village(id = villageId, name = "1마을", department = mockk())
        val otherVillage = Village(id = otherVillageId, name = "2마을", department = mockk())
        // 다른 마을에 속한 GBS
        val gbsGroup = GbsGroup(id = gbsId, name = "GBS1", village = otherVillage, termStartDate = startDate, termEndDate = endDate)
        
        // 요청 객체 생성
        val request = GbsAssignmentSaveRequest(
            startDate = startDate,
            endDate = endDate,
            assignments = listOf(
                GbsAssignment(gbsId = gbsId, leaderId = leaderId, memberIds = emptyList())
            )
        )
        
        // Mock 설정
        every { villageRepository.findById(villageId) } returns java.util.Optional.of(village)
        every { gbsGroupRepository.findById(gbsId) } returns java.util.Optional.of(gbsGroup)
        
        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.saveGbsAssignment(villageId, request)
        }
        
        assertEquals(ErrorMessage.GBS_GROUP_NOT_IN_VILLAGE.code, exception.errorMessage.code)
    }
    
    @Test
    fun `saveGbsAssignment는 존재하지 않는 마을에 대한 요청을 거부한다`() {
        // Given
        val villageId = 999L
        val startDate = LocalDate.of(2023, 6, 1)
        val endDate = LocalDate.of(2023, 12, 31)
        
        // 요청 객체 생성
        val request = GbsAssignmentSaveRequest(
            startDate = startDate,
            endDate = endDate,
            assignments = emptyList()
        )
        
        // Mock 설정
        every { villageRepository.findById(villageId) } returns java.util.Optional.empty()
        
        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            gbsMemberService.saveGbsAssignment(villageId, request)
        }
        
        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.code, exception.errorMessage.code)
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