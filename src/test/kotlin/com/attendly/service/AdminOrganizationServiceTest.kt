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
import io.mockk.MockKAnnotations

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

    @MockK
    private lateinit var villageLeaderRepository: VillageLeaderRepository

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
        villageLeaderRepository = mockk(relaxed = true)
        
        adminOrganizationService = AdminOrganizationService(
            departmentRepository,
            villageRepository,
            gbsGroupRepository,
            userRepository,
            gbsLeaderHistoryRepository,
            gbsMemberHistoryRepository,
            villageLeaderRepository
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

    @Test
    fun `createVillage with villageLeaderId - new leader assigned successfully`() {
        // Given
        val departmentId = 1L
        val villageLeaderId = 2L
        val request = VillageCreateRequest(
            name = "Test Village",
            departmentId = departmentId,
            villageLeaderId = villageLeaderId
        )

        val department = Department(id = departmentId, name = "Test Department")
        val village = Village(id = 1L, name = "Test Village", department = department)
        val user = User(id = villageLeaderId, name = "Test Leader", email = "leader@test.com", role = Role.MEMBER, department = department)

        every { departmentRepository.findById(departmentId) } returns Optional.of(department)
        every { villageRepository.save(any()) } returns village
        every { userRepository.findById(villageLeaderId) } returns Optional.of(user)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(village.id!!) } returns null
        every { villageLeaderRepository.save(any()) } returns mockk()

        // When
        val result = adminOrganizationService.createVillage(request)

        // Then
        assertEquals(village.id, result.id)
        assertEquals(village.name, result.name)
        assertEquals(department.id, result.departmentId)
        assertEquals(department.name, result.departmentName)
        assertEquals(user.id, result.villageLeaderId)
        assertEquals(user.name, result.villageLeaderName)

        verify { departmentRepository.findById(departmentId) }
        verify { villageRepository.save(any()) }
        verify { userRepository.findById(villageLeaderId) }
        verify { villageLeaderRepository.findByVillageIdAndEndDateIsNull(village.id!!) }
        verify { villageLeaderRepository.save(any()) }
    }

    @Test
    fun `createVillage with villageLeaderId - replace existing leader`() {
        // Given
        val departmentId = 1L
        val existingLeaderId = 2L
        val newLeaderId = 3L
        val request = VillageCreateRequest(
            name = "Test Village",
            departmentId = departmentId,
            villageLeaderId = newLeaderId
        )

        val department = Department(id = departmentId, name = "Test Department")
        val village = Village(id = 1L, name = "Test Village", department = department)
        val existingLeader = User(id = existingLeaderId, name = "Existing Leader", email = "existing@test.com", role = Role.MEMBER, department = department)
        val newLeader = User(id = newLeaderId, name = "New Leader", email = "new@test.com", role = Role.MEMBER, department = department)
        
        val existingVillageLeader = VillageLeader(
            user = existingLeader,
            village = village,
            startDate = LocalDate.now().minusDays(10)
        )

        every { departmentRepository.findById(departmentId) } returns Optional.of(department)
        every { villageRepository.save(any()) } returns village
        every { userRepository.findById(newLeaderId) } returns Optional.of(newLeader)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(village.id!!) } returns existingVillageLeader
        every { villageLeaderRepository.save(any()) } returns mockk()

        // When
        val result = adminOrganizationService.createVillage(request)

        // Then
        assertEquals(village.id, result.id)
        assertEquals(village.name, result.name)
        assertEquals(department.id, result.departmentId)
        assertEquals(department.name, result.departmentName)
        assertEquals(newLeader.id, result.villageLeaderId)
        assertEquals(newLeader.name, result.villageLeaderName)

        verify { departmentRepository.findById(departmentId) }
        verify { villageRepository.save(any()) }
        verify { userRepository.findById(newLeaderId) }
        verify { villageLeaderRepository.findByVillageIdAndEndDateIsNull(village.id!!) }
        verify(exactly = 2) { villageLeaderRepository.save(any()) } // 기존 리더 종료 + 새 리더 추가
    }

    @Test
    fun `updateVillage with villageLeaderId - replace existing leader`() {
        // Given
        val villageId = 1L
        val departmentId = 1L
        val existingLeaderId = 2L
        val newLeaderId = 3L
        val request = VillageUpdateRequest(
            name = "Updated Village",
            departmentId = null,
            villageLeaderId = newLeaderId
        )

        val department = Department(id = departmentId, name = "Test Department")
        val village = Village(id = villageId, name = "Test Village", department = department)
        val updatedVillage = Village(id = villageId, name = "Updated Village", department = department)
        val existingLeader = User(id = existingLeaderId, name = "Existing Leader", email = "existing@test.com", role = Role.MEMBER, department = department)
        val newLeader = User(id = newLeaderId, name = "New Leader", email = "new@test.com", role = Role.MEMBER, department = department)
        
        val existingVillageLeader = VillageLeader(
            user = existingLeader,
            village = village,
            startDate = LocalDate.now().minusDays(10)
        )

        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { villageRepository.save(any()) } returns updatedVillage
        every { userRepository.findById(newLeaderId) } returns Optional.of(newLeader)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) } returns existingVillageLeader
        every { villageLeaderRepository.save(any()) } returns mockk()

        // When
        val result = adminOrganizationService.updateVillage(villageId, request)

        // Then
        assertEquals(updatedVillage.id, result.id)
        assertEquals(updatedVillage.name, result.name)
        assertEquals(department.id, result.departmentId)
        assertEquals(department.name, result.departmentName)
        assertEquals(newLeader.id, result.villageLeaderId)
        assertEquals(newLeader.name, result.villageLeaderName)

        verify { villageRepository.findById(villageId) }
        verify { villageRepository.save(any()) }
        verify { userRepository.findById(newLeaderId) }
        verify { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) }
        verify(exactly = 2) { villageLeaderRepository.save(any()) } // 기존 리더 종료 + 새 리더 추가
    }

    @Test
    fun `updateVillage with no villageLeaderId - keep existing leader`() {
        // Given
        val villageId = 1L
        val departmentId = 1L
        val existingLeaderId = 2L
        val request = VillageUpdateRequest(
            name = "Updated Village",
            departmentId = null,
            villageLeaderId = null
        )

        val department = Department(id = departmentId, name = "Test Department")
        val village = Village(id = villageId, name = "Test Village", department = department)
        val updatedVillage = Village(id = villageId, name = "Updated Village", department = department)
        val existingLeader = User(id = existingLeaderId, name = "Existing Leader", email = "existing@test.com", role = Role.MEMBER, department = department)
        
        val existingVillageLeader = VillageLeader(
            user = existingLeader,
            village = village,
            startDate = LocalDate.now().minusDays(10)
        )

        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { villageRepository.save(any()) } returns updatedVillage
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) } returns existingVillageLeader

        // When
        val result = adminOrganizationService.updateVillage(villageId, request)

        // Then
        assertEquals(updatedVillage.id, result.id)
        assertEquals(updatedVillage.name, result.name)
        assertEquals(department.id, result.departmentId)
        assertEquals(department.name, result.departmentName)
        assertEquals(existingLeader.id, result.villageLeaderId)
        assertEquals(existingLeader.name, result.villageLeaderName)

        verify { villageRepository.findById(villageId) }
        verify { villageRepository.save(any()) }
        verify { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) }
        verify(exactly = 0) { villageLeaderRepository.save(any()) } // 변경 사항 없음
    }

    @Test
    @DisplayName("모든 GBS 그룹 조회 - 성공")
    fun getAllGbsGroups_Success() {
        // given
        val now = LocalDateTime.now()
        
        val gbsQueryDto1 = AdminGbsGroupQueryDto(
            id = 1L,
            name = "믿음 GBS",
            villageId = 1L,
            villageName = "1마을",
            termStartDate = LocalDate.of(2024, 1, 1),
            termEndDate = LocalDate.of(2024, 6, 30),
            leaderId = 101L,
            leaderName = "김리더",
            createdAt = now,
            updatedAt = now,
            memberCount = 5L
        )
        
        val gbsQueryDto2 = AdminGbsGroupQueryDto(
            id = 2L,
            name = "소망 GBS",
            villageId = 1L,
            villageName = "1마을",
            termStartDate = LocalDate.of(2024, 1, 1),
            termEndDate = LocalDate.of(2024, 6, 30),
            leaderId = null,
            leaderName = null,
            createdAt = now,
            updatedAt = now,
            memberCount = 3L
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("id").descending())
        val page = PageImpl(listOf(gbsQueryDto1, gbsQueryDto2), pageable, 2)
        
        every { gbsGroupRepository.findAllGbsGroupsWithCompleteDetails(pageable) } returns page
        
        // when
        val result = adminOrganizationService.getAllGbsGroups(pageable)
        
        // then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals(2, result.totalCount)
        assertFalse(result.hasMore)
        
        val firstGbs = result.items[0]
        assertEquals(1L, firstGbs.id)
        assertEquals("믿음 GBS", firstGbs.name)
        assertEquals(1L, firstGbs.villageId)
        assertEquals("1마을", firstGbs.villageName)
        assertEquals(LocalDate.of(2024, 1, 1), firstGbs.termStartDate)
        assertEquals(LocalDate.of(2024, 6, 30), firstGbs.termEndDate)
        assertEquals(101L, firstGbs.leaderId)
        assertEquals("김리더", firstGbs.leaderName)
        assertEquals(5, firstGbs.memberCount)
        assertEquals(now, firstGbs.createdAt)
        assertEquals(now, firstGbs.updatedAt)
        
        val secondGbs = result.items[1]
        assertEquals(2L, secondGbs.id)
        assertEquals("소망 GBS", secondGbs.name)
        assertEquals(1L, secondGbs.villageId)
        assertEquals("1마을", secondGbs.villageName)
        assertEquals(LocalDate.of(2024, 1, 1), secondGbs.termStartDate)
        assertEquals(LocalDate.of(2024, 6, 30), secondGbs.termEndDate)
        assertNull(secondGbs.leaderId)
        assertNull(secondGbs.leaderName)
        assertEquals(3, secondGbs.memberCount)
        assertEquals(now, secondGbs.createdAt)
        assertEquals(now, secondGbs.updatedAt)
        
        verify(exactly = 1) { gbsGroupRepository.findAllGbsGroupsWithCompleteDetails(pageable) }
    }

    @Test
    @DisplayName("모든 GBS 그룹 조회 - 빈 결과")
    fun getAllGbsGroups_EmptyResult() {
        // given
        val pageable = PageRequest.of(0, 20, Sort.by("id").descending())
        val emptyPage = PageImpl<AdminGbsGroupQueryDto>(emptyList(), pageable, 0)
        
        every { gbsGroupRepository.findAllGbsGroupsWithCompleteDetails(pageable) } returns emptyPage
        
        // when
        val result = adminOrganizationService.getAllGbsGroups(pageable)
        
        // then
        assertNotNull(result)
        assertEquals(0, result.items.size)
        assertEquals(0, result.totalCount)
        assertFalse(result.hasMore)
        
        verify(exactly = 1) { gbsGroupRepository.findAllGbsGroupsWithCompleteDetails(pageable) }
    }
} 