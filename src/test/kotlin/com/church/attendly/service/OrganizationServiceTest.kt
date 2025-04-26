package com.church.attendly.service

import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.entity.Village
import com.church.attendly.domain.repository.DepartmentRepository
import com.church.attendly.domain.repository.GbsGroupRepository
import com.church.attendly.domain.repository.GbsLeaderHistoryRepository
import com.church.attendly.domain.repository.VillageRepository
import com.church.attendly.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class OrganizationServiceTest {

    @Mock
    private lateinit var departmentRepository: DepartmentRepository

    @Mock
    private lateinit var villageRepository: VillageRepository

    @Mock
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @Mock
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    @InjectMocks
    private lateinit var organizationService: OrganizationService

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var leader: User

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
        
        leader = User(
            id = 1L,
            name = "홍길동",
            role = com.church.attendly.domain.entity.Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    fun `getAllDepartments 모든 부서를 반환해야 함`() {
        // given
        val departments = listOf(department)
        `when`(departmentRepository.findAll()).thenReturn(departments)

        // when
        val result = organizationService.getAllDepartments()

        // then
        assertEquals(departments, result)
    }

    @Test
    fun `getDepartmentById 유효한 ID로 부서를 반환해야 함`() {
        // given
        `when`(departmentRepository.findById(1L)).thenReturn(Optional.of(department))

        // when
        val result = organizationService.getDepartmentById(1L)

        // then
        assertEquals(department, result)
    }

    @Test
    fun `getDepartmentById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        `when`(departmentRepository.findById(999L)).thenReturn(Optional.empty())

        // then
        assertThrows(ResourceNotFoundException::class.java) {
            // when
            organizationService.getDepartmentById(999L)
        }
    }

    @Test
    fun `getVillagesByDepartment 부서에 속한 마을들을 반환해야 함`() {
        // given
        val villages = listOf(village)
        `when`(departmentRepository.findById(1L)).thenReturn(Optional.of(department))
        `when`(villageRepository.findByDepartment(department)).thenReturn(villages)

        // when
        val result = organizationService.getVillagesByDepartment(1L)

        // then
        assertEquals(villages, result)
    }

    @Test
    fun `getVillageById 유효한 ID로 마을을 반환해야 함`() {
        // given
        `when`(villageRepository.findById(1L)).thenReturn(Optional.of(village))

        // when
        val result = organizationService.getVillageById(1L)

        // then
        assertEquals(village, result)
    }

    @Test
    fun `getVillageById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        `when`(villageRepository.findById(999L)).thenReturn(Optional.empty())

        // then
        assertThrows(ResourceNotFoundException::class.java) {
            // when
            organizationService.getVillageById(999L)
        }
    }

    @Test
    fun `getGbsGroupsByVillage 마을에 속한 GBS 그룹들을 반환해야 함`() {
        // given
        val gbsGroups = listOf(gbsGroup)
        `when`(villageRepository.findById(1L)).thenReturn(Optional.of(village))
        `when`(gbsGroupRepository.findByVillage(village)).thenReturn(gbsGroups)

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
        `when`(gbsGroupRepository.findActiveGroupsByVillageId(1L, date)).thenReturn(gbsGroups)

        // when
        val result = organizationService.getActiveGbsGroupsByVillage(1L, date)

        // then
        assertEquals(gbsGroups, result)
    }

    @Test
    fun `getGbsGroupById 유효한 ID로 GBS 그룹을 반환해야 함`() {
        // given
        `when`(gbsGroupRepository.findById(1L)).thenReturn(Optional.of(gbsGroup))

        // when
        val result = organizationService.getGbsGroupById(1L)

        // then
        assertEquals(gbsGroup, result)
    }

    @Test
    fun `getGbsGroupById 유효하지 않은 ID로 예외를 발생시켜야 함`() {
        // given
        `when`(gbsGroupRepository.findById(999L)).thenReturn(Optional.empty())

        // then
        assertThrows(ResourceNotFoundException::class.java) {
            // when
            organizationService.getGbsGroupById(999L)
        }
    }

    @Test
    fun `getCurrentLeaderForGbs 현재 GBS 리더를 반환해야 함`() {
        // given
        `when`(gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(1L)).thenReturn(leader)

        // when
        val result = organizationService.getCurrentLeaderForGbs(1L)

        // then
        assertEquals("홍길동", result)
    }

    @Test
    fun `getCurrentLeaderForGbs 리더가 없을 때 예외를 발생시켜야 함`() {
        // given
        `when`(gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(999L)).thenReturn(null)

        // then
        assertThrows(ResourceNotFoundException::class.java) {
            // when
            organizationService.getCurrentLeaderForGbs(999L)
        }
    }
} 