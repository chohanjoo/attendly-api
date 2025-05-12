package com.attendly.service

import com.attendly.api.dto.*
import com.attendly.domain.entity.*
import com.attendly.domain.repository.*
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.VillageResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageRepository
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.junit.jupiter.api.DisplayName
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AdminOrganizationServiceTest {

    @MockK
    private lateinit var departmentRepository: DepartmentRepository

    @MockK
    private lateinit var villageRepository: VillageRepository

    @MockK
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @MockK
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @InjectMockKs
    private lateinit var adminOrganizationService: AdminOrganizationService

    @BeforeEach
    fun setUp() {
        departmentRepository = mockk(relaxed = true)
        villageRepository = mockk(relaxed = true)
        gbsGroupRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        gbsLeaderHistoryRepository = mockk(relaxed = true)
        gbsMemberHistoryRepository = mockk(relaxed = true)
        
        adminOrganizationService = AdminOrganizationService(
            departmentRepository,
            villageRepository,
            gbsGroupRepository,
            userRepository,
            gbsLeaderHistoryRepository,
            gbsMemberHistoryRepository
        )
    }

    @Test
    fun `updateDepartment should throw exception when department not found`() {
        // given
        val departmentId = 999L
        val request = DepartmentUpdateRequest(name = "새로운 부서명")
        
        every { departmentRepository.findById(departmentId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.updateDepartment(departmentId, request)
        }
        
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId), exception.message)
        
        verify { departmentRepository.findById(departmentId) }
    }

    @Test
    fun `deleteDepartment should throw exception when department not found`() {
        // given
        val departmentId = 999L
        
        every { departmentRepository.existsById(departmentId) } returns false

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.deleteDepartment(departmentId)
        }
        
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId), exception.message)
        
        verify { departmentRepository.existsById(departmentId) }
    }

    @Test
    fun `getDepartment should throw exception when department not found`() {
        // given
        val departmentId = 999L
        
        every { departmentRepository.findById(departmentId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.getDepartment(departmentId)
        }
        
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, departmentId), exception.message)
        
        verify { departmentRepository.findById(departmentId) }
    }

    @Test
    fun `createVillage should throw exception when department not found`() {
        // given
        val request = VillageCreateRequest(
            name = "새 마을",
            departmentId = 999L,
            villageLeaderId = null
        )
        
        every { departmentRepository.findById(999L) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.createVillage(request)
        }
        
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId), exception.message)
        
        verify { departmentRepository.findById(999L) }
    }

    @Test
    fun `updateVillage should throw exception when village not found`() {
        // given
        val villageId = 999L
        val request = VillageUpdateRequest(name = "마을 이름 변경")
        
        every { villageRepository.findById(villageId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.updateVillage(villageId, request)
        }
        
        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, villageId), exception.message)
        
        verify { villageRepository.findById(villageId) }
    }

    @Test
    fun `createGbsGroup should throw exception when village not found`() {
        // given
        val request = GbsGroupCreateRequest(
            name = "새 GBS",
            villageId = 999L,
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6),
            leaderId = null
        )
        
        every { villageRepository.findById(999L) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.createGbsGroup(request)
        }
        
        assertEquals(ErrorMessage.VILLAGE_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.VILLAGE_NOT_FOUND, request.villageId), exception.message)
        
        verify { villageRepository.findById(999L) }
    }

    @Test
    fun `updateGbsGroup should throw exception when gbs group not found`() {
        // given
        val gbsGroupId = 999L
        val request = GbsGroupUpdateRequest(name = "GBS 이름 변경")
        
        every { gbsGroupRepository.findById(gbsGroupId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.updateGbsGroup(gbsGroupId, request)
        }
        
        assertEquals(ErrorMessage.GBS_GROUP_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsGroupId), exception.message)
        
        verify { gbsGroupRepository.findById(gbsGroupId) }
    }

    @Test
    fun `assignLeaderToGbs should throw exception when gbs group not found`() {
        // given
        val gbsGroupId = 999L
        val request = GbsLeaderAssignRequest(
            leaderId = 1L,
            startDate = LocalDate.now()
        )
        
        every { gbsGroupRepository.findById(gbsGroupId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.assignLeaderToGbs(gbsGroupId, request)
        }
        
        assertEquals(ErrorMessage.GBS_GROUP_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.GBS_GROUP_NOT_FOUND, gbsGroupId), exception.message)
        
        verify { gbsGroupRepository.findById(gbsGroupId) }
    }

    @Test
    fun `assignLeaderToGbs should throw exception when leader not found`() {
        // given
        val gbsGroupId = 1L
        val leaderId = 999L
        val request = GbsLeaderAssignRequest(
            leaderId = leaderId,
            startDate = LocalDate.now()
        )
        
        val gbsGroup = GbsGroup(
            id = gbsGroupId,
            name = "1GBS",
            village = Village(id = 1L, name = "1마을", department = Department(id = 1L, name = "청년부")),
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31)
        )
        
        every { gbsGroupRepository.findById(gbsGroupId) } returns Optional.of(gbsGroup)
        every { userRepository.findById(leaderId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.assignLeaderToGbs(gbsGroupId, request)
        }
        
        assertEquals(ErrorMessage.USER_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, leaderId), exception.message)
        
        verify { gbsGroupRepository.findById(gbsGroupId) }
        verify { userRepository.findById(leaderId) }
    }

    @Test
    fun `executeGbsReorganization should throw exception when department not found`() {
        // given
        val request = GbsReorganizationRequest(
            departmentId = 999L,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusMonths(6)
        )
        
        every { departmentRepository.findById(request.departmentId) } returns Optional.empty()

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminOrganizationService.executeGbsReorganization(request)
        }
        
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND.code, exception.errorMessage.code)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.DEPARTMENT_NOT_FOUND, request.departmentId), exception.message)
        
        verify { departmentRepository.findById(request.departmentId) }
    }

    @Test
    @DisplayName("마을 목록 조회 - 필터 없음")
    fun getAllVillagesWithoutFilter() {
        // given
        val now = LocalDateTime.now()
        val department = Department(id = 1L, name = "청년부", createdAt = now, updatedAt = now)
        
        val user1 = User(
            id = 101L, 
            name = "김철수", 
            email = "user1@example.com", 
            password = "password", 
            role = Role.VILLAGE_LEADER,
            department = department,
            createdAt = now, 
            updatedAt = now
        )
        
        val user2 = User(
            id = 102L, 
            name = "이영희", 
            email = "user2@example.com", 
            password = "password", 
            role = Role.VILLAGE_LEADER,
            department = department,
            createdAt = now, 
            updatedAt = now
        )
        
        // 목(mock) 객체로 생성
        val village1 = mockk<Village>()
        val village2 = mockk<Village>()
        
        val villageLeader1 = mockk<VillageLeader>()
        val villageLeader2 = mockk<VillageLeader>()
        
        // 필요한 속성 설정
        every { village1.id } returns 1L
        every { village1.name } returns "동문마을"
        every { village1.department } returns department
        every { village1.villageLeader } returns villageLeader1
        every { village1.createdAt } returns now
        every { village1.updatedAt } returns now
        
        every { village2.id } returns 2L
        every { village2.name } returns "서문마을"
        every { village2.department } returns department
        every { village2.villageLeader } returns villageLeader2
        every { village2.createdAt } returns now
        every { village2.updatedAt } returns now
        
        every { villageLeader1.user } returns user1
        every { villageLeader2.user } returns user2
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        val page = PageImpl(listOf(village1, village2), pageable, 2)
        
        every { 
            villageRepository.findVillagesWithParams(null, null, pageable) 
        } returns page
        
        // when
        val result = adminOrganizationService.getAllVillages(null, null, pageable)
        
        // then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(2, result.totalCount)
        assertFalse(result.hasMore)
        
        val firstVillage = result.items[0]
        assertEquals(1L, firstVillage.id)
        assertEquals("동문마을", firstVillage.name)
        assertEquals(1L, firstVillage.departmentId)
        assertEquals("청년부", firstVillage.departmentName)
        assertEquals(101L, firstVillage.villageLeaderId)
        assertEquals("김철수", firstVillage.villageLeaderName)
        
        verify(exactly = 1) { villageRepository.findVillagesWithParams(null, null, pageable) }
    }
    
    @Test
    @DisplayName("마을 목록 조회 - 부서 ID 필터링")
    fun getAllVillagesWithDepartmentFilter() {
        // given
        val departmentId = 1L
        val now = LocalDateTime.now()
        val department = Department(id = departmentId, name = "청년부", createdAt = now, updatedAt = now)
        
        val user = User(
            id = 101L, 
            name = "김철수", 
            email = "user1@example.com", 
            password = "password", 
            role = Role.VILLAGE_LEADER,
            department = department,
            createdAt = now, 
            updatedAt = now
        )
        
        // 목(mock) 객체로 생성
        val village = mockk<Village>()
        val villageLeader = mockk<VillageLeader>()
        
        // 필요한 속성 설정
        every { village.id } returns 1L
        every { village.name } returns "동문마을"
        every { village.department } returns department
        every { village.villageLeader } returns villageLeader
        every { village.createdAt } returns now
        every { village.updatedAt } returns now
        
        every { villageLeader.user } returns user
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        val page = PageImpl(listOf(village), pageable, 1)
        
        every { 
            villageRepository.findVillagesWithParams(departmentId, null, pageable) 
        } returns page
        
        // when
        val result = adminOrganizationService.getAllVillages(departmentId, null, pageable)
        
        // then
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals(1, result.totalCount)
        assertFalse(result.hasMore)
        
        val villageResponse = result.items[0]
        assertEquals(1L, villageResponse.id)
        assertEquals("동문마을", villageResponse.name)
        assertEquals(departmentId, villageResponse.departmentId)
        assertEquals("청년부", villageResponse.departmentName)
        
        verify(exactly = 1) { villageRepository.findVillagesWithParams(departmentId, null, pageable) }
    }
    
    @Test
    @DisplayName("마을 목록 조회 - 이름 필터링")
    fun getAllVillagesWithNameFilter() {
        // given
        val name = "동문"
        val now = LocalDateTime.now()
        val department = Department(id = 1L, name = "청년부", createdAt = now, updatedAt = now)
        
        val user = User(
            id = 101L, 
            name = "김철수", 
            email = "user1@example.com", 
            password = "password", 
            role = Role.VILLAGE_LEADER,
            department = department,
            createdAt = now, 
            updatedAt = now
        )
        
        // 목(mock) 객체로 생성
        val village = mockk<Village>()
        val villageLeader = mockk<VillageLeader>()
        
        // 필요한 속성 설정
        every { village.id } returns 1L
        every { village.name } returns "동문마을"
        every { village.department } returns department
        every { village.villageLeader } returns villageLeader
        every { village.createdAt } returns now
        every { village.updatedAt } returns now
        
        every { villageLeader.user } returns user
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        val page = PageImpl(listOf(village), pageable, 1)
        
        every { 
            villageRepository.findVillagesWithParams(null, name, pageable) 
        } returns page
        
        // when
        val result = adminOrganizationService.getAllVillages(null, name, pageable)
        
        // then
        assertNotNull(result)
        assertEquals(1, result.items.size)
        assertEquals(1, result.totalCount)
        assertFalse(result.hasMore)
        
        val villageResponse = result.items[0]
        assertEquals(1L, villageResponse.id)
        assertEquals("동문마을", villageResponse.name)
        assertEquals("김철수", villageResponse.villageLeaderName)
        
        verify(exactly = 1) { villageRepository.findVillagesWithParams(null, name, pageable) }
    }
} 