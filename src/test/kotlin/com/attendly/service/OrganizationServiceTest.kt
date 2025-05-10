package com.attendly.service

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import com.attendly.domain.model.GbsWithLeader
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class OrganizationServiceTest {

    @MockK
    private lateinit var departmentRepository: DepartmentRepository

    @MockK
    private lateinit var villageRepository: VillageRepository

    @MockK
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @MockK
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository
    
    @MockK
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @InjectMockKs
    private lateinit var organizationService: OrganizationService

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var gbsGroup2: GbsGroup
    private lateinit var leader: User
    private lateinit var member1: User
    private lateinit var member2: User
    private lateinit var leaderHistory: GbsLeaderHistory
    private lateinit var memberHistory1: GbsMemberHistory
    private lateinit var memberHistory2: GbsMemberHistory

    @BeforeEach
    fun setUp() {
        department = Department(
            id = 1L, 
            name = "청년부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        village = Village(
            id = 1L, 
            name = "1마을", 
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        gbsGroup = GbsGroup(
            id = 1L, 
            name = "1GBS", 
            village = village, 
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        gbsGroup2 = GbsGroup(
            id = 2L, 
            name = "2GBS", 
            village = village, 
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        leader = User(
            id = 1L,
            name = "홍길동",
            role = com.attendly.domain.entity.Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        member1 = User(
            id = 2L,
            name = "김조원",
            role = com.attendly.domain.entity.Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        member2 = User(
            id = 3L,
            name = "이조원",
            role = com.attendly.domain.entity.Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        leaderHistory = GbsLeaderHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        memberHistory1 = GbsMemberHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            member = member1,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        memberHistory2 = GbsMemberHistory(
            id = 2L,
            gbsGroup = gbsGroup,
            member = member2,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `getAllDepartments 모든 부서를 반환해야 함`() {
        // given
        val departments = listOf(department)
        every { departmentRepository.findAll() } returns departments

        // when
        val result = organizationService.getAllDepartments()

        // then
        assertEquals(departments, result)
    }

    @Test
    fun `getDepartmentById 유효한 ID로 부서를 반환해야 함`() {
        // given
        every { departmentRepository.findById(1L) } returns Optional.of(department)

        // when
        val result = organizationService.getDepartmentById(1L)

        // then
        assertEquals(department, result)
    }

    @Test
    fun `getDepartmentById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        every { departmentRepository.findById(999L) } returns Optional.empty()

        // then
        val exception = assertThrows(AttendlyApiException::class.java) {
            // when
            organizationService.getDepartmentById(999L)
        }
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
    }

    @Test
    fun `getVillagesByDepartment 부서에 속한 마을들을 반환해야 함`() {
        // given
        val villages = listOf(village)
        every { departmentRepository.findById(1L) } returns Optional.of(department)
        every { villageRepository.findByDepartment(department) } returns villages

        // when
        val result = organizationService.getVillagesByDepartment(1L)

        // then
        assertEquals(villages, result)
    }

    @Test
    fun `getVillageById 유효한 ID로 마을을 반환해야 함`() {
        // given
        every { villageRepository.findById(1L) } returns Optional.of(village)

        // when
        val result = organizationService.getVillageById(1L)

        // then
        assertEquals(village, result)
    }

    @Test
    fun `getVillageById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        every { villageRepository.findById(999L) } returns Optional.empty()

        // then
        val exception = assertThrows(AttendlyApiException::class.java) {
            // when
            organizationService.getVillageById(999L)
        }
        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.code, exception.errorMessage.code)
    }

    @Test
    fun `getGbsGroupsByVillage 마을에 속한 GBS 그룹들을 반환해야 함`() {
        // given
        val gbsGroups = listOf(gbsGroup)
        every { villageRepository.findById(1L) } returns Optional.of(village)
        every { gbsGroupRepository.findByVillage(village) } returns gbsGroups

        // when
        val result = organizationService.getGbsGroupsByVillage(1L)

        // then
        assertEquals(gbsGroups, result)
    }

    @Test
    fun `getActiveGbsGroupsByVillage 특정 날짜에 활성화된 GBS 그룹들을 반환해야 함`() {
        // given
        val date = LocalDate.now()
        val gbsGroups = listOf(gbsGroup)
        every { gbsGroupRepository.findActiveGroupsByVillageId(1L, date) } returns gbsGroups

        // when
        val result = organizationService.getActiveGbsGroupsByVillage(1L, date)

        // then
        assertEquals(gbsGroups, result)
    }

    @Test
    fun `getGbsGroupById 유효한 ID로 GBS 그룹을 반환해야 함`() {
        // given
        every { gbsGroupRepository.findById(1L) } returns Optional.of(gbsGroup)

        // when
        val result = organizationService.getGbsGroupById(1L)

        // then
        assertEquals(gbsGroup, result)
    }

    @Test
    fun `getGbsGroupById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        every { gbsGroupRepository.findById(999L) } returns Optional.empty()

        // then
        val exception = assertThrows(AttendlyApiException::class.java) {
            // when
            organizationService.getGbsGroupById(999L)
        }
        assertEquals(ErrorMessage.GBS_GROUP_NOT_FOUND.code, exception.errorMessage.code)
    }

    @Test
    fun `getCurrentLeaderForGbs 현재 GBS 리더를 반환해야 함`() {
        // given
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L) } returns leader

        // when
        val result = organizationService.getCurrentLeaderForGbs(1L)

        // then
        assertEquals("홍길동", result)
    }

    @Test
    fun `getCurrentLeaderForGbs 리더가 없을 때 예외를 발생시켜야 함`() {
        // given
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(999L) } returns null

        // then
        val exception = assertThrows(AttendlyApiException::class.java) {
            // when
            organizationService.getCurrentLeaderForGbs(999L)
        }
        assertEquals(ErrorMessage.NO_ACTIVE_LEADER.code, exception.errorMessage.code)
    }
    
    @Test
    fun `getVillageGbsInfo 마을의 모든 GBS 정보를 반환해야 함`() {
        // given
        val date = LocalDate.now()
        val gbsGroups = listOf(gbsGroup, gbsGroup2)
        val condition1 = GbsMemberHistorySearchCondition(
            gbsId = 1L,
            startDate = date,
            endDate = date
        )
        val condition2 = GbsMemberHistorySearchCondition(
            gbsId = 2L,
            startDate = date,
            endDate = date
        )
        val memberHistories = listOf(memberHistory1, memberHistory2)
        
        every { villageRepository.findById(1L) } returns Optional.of(village)
        every { gbsGroupRepository.findActiveGroupsByVillageId(1L, date) } returns gbsGroups
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L) } returns leader
        every { gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(2L) } returns null
        every { gbsMemberHistoryRepository.findActiveMembers(condition1) } returns memberHistories
        every { gbsMemberHistoryRepository.findActiveMembers(condition2) } returns emptyList()
        
        // when
        val result = organizationService.getVillageGbsInfo(1L, date)
        
        // then
        assertEquals(1L, result.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(2, result.gbsCount)
        assertEquals(2, result.totalMemberCount)
        assertEquals(2, result.gbsList.size)
        
        val gbsInfo1 = result.gbsList.find { it.gbsId == 1L }
        assertNotNull(gbsInfo1)
        assertEquals("1GBS", gbsInfo1!!.gbsName)
        assertEquals(1L, gbsInfo1.leaderId)
        assertEquals("홍길동", gbsInfo1.leaderName)
        assertEquals(2, gbsInfo1.memberCount)
        assertEquals(2, gbsInfo1.members.size)
        
        val gbsInfo2 = result.gbsList.find { it.gbsId == 2L }
        assertNotNull(gbsInfo2)
        assertEquals("2GBS", gbsInfo2!!.gbsName)
        assertNull(gbsInfo2.leaderId)
        assertEquals("리더 없음", gbsInfo2.leaderName)
        assertEquals(0, gbsInfo2.memberCount)
        assertEquals(0, gbsInfo2.members.size)
    }

    @Test
    fun `getGbsWithLeader - GBS 그룹과 리더 정보를 함께 조회한다`() {
        // given
        val gbsId = 1L
        val gbsGroup = GbsGroup(
            id = gbsId,
            name = "테스트 GBS",
            village = mockk(),
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6)
        )
        val leader = User(
            id = 2L,
            name = "테스트 리더",
            role = com.attendly.domain.entity.Role.LEADER,
            department = mockk()
        )
        val gbsWithLeader = GbsWithLeader(gbsGroup, leader)
        
        every { gbsGroupRepository.findWithCurrentLeader(gbsId) } returns gbsWithLeader
        
        // when
        val result = organizationService.getGbsWithLeader(gbsId)
        
        // then
        assertEquals(gbsGroup, result.first)
        assertEquals("테스트 리더", result.second)
    }
    
    @Test
    fun `getGbsWithLeader - 리더가 없는 경우 그룹 정보만 조회된다`() {
        // given
        val gbsId = 1L
        val gbsGroup = GbsGroup(
            id = gbsId,
            name = "테스트 GBS",
            village = mockk(),
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6)
        )
        val gbsWithLeader = GbsWithLeader(gbsGroup, null)
        
        every { gbsGroupRepository.findWithCurrentLeader(gbsId) } returns gbsWithLeader
        
        // when
        val result = organizationService.getGbsWithLeader(gbsId)
        
        // then
        assertEquals(gbsGroup, result.first)
        assertNull(result.second)
    }
    
    @Test
    fun `getGbsWithLeader - GBS 그룹이 없는 경우 예외가 발생한다`() {
        // given
        val gbsId = 1L
        every { gbsGroupRepository.findWithCurrentLeader(gbsId) } returns null
        
        // when, then
        val exception = assertThrows(AttendlyApiException::class.java) {
            organizationService.getGbsWithLeader(gbsId)
        }
    }
} 