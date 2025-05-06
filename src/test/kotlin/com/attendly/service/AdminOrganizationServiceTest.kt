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

class AdminOrganizationServiceTest {

    private lateinit var departmentRepository: DepartmentRepository
    private lateinit var villageRepository: VillageRepository
    private lateinit var gbsGroupRepository: GbsGroupRepository
    private lateinit var userRepository: UserRepository
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository
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
} 