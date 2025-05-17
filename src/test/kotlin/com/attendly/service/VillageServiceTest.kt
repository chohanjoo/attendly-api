package com.attendly.service

import com.attendly.api.dto.UserVillageResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.security.UserDetailsAdapter
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VillageServiceTest {

    private lateinit var villageService: VillageService
    private lateinit var villageRepository: VillageRepository
    private lateinit var villageLeaderRepository: VillageLeaderRepository
    private lateinit var authentication: Authentication
    private lateinit var userDetailsAdapter: UserDetailsAdapter
    private lateinit var user: User
    
    @BeforeEach
    fun setUp() {
        villageRepository = mockk()
        villageLeaderRepository = mockk()
        authentication = mockk()
        userDetailsAdapter = mockk()
        user = mockk()
        
        villageService = VillageService(
            villageRepository = villageRepository,
            villageLeaderRepository = villageLeaderRepository
        )
    }
    
    @Test
    fun `마을 ID로 마을 정보를 조회한다`() {
        // given
        val villageId = 1L
        val departmentId = 1L
        val userId = 1L
        
        val department = Department(
            id = departmentId,
            name = "대학부"
        )
        
        val village = Village(
            id = villageId,
            name = "1마을",
            department = department
        )
        
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) } returns null
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = villageService.getVillageById(villageId, authentication)
        
        // then
        assertEquals(villageId, result.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(departmentId, result.departmentId)
        assertEquals("대학부", result.departmentName)
        assertFalse(result.isVillageLeader)
        
        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) }
    }
    
    @Test
    fun `마을 ID로 마을 정보를 조회할 때 로그인한 사용자가 마을장인 경우 isVillageLeader가 true이다`() {
        // given
        val villageId = 1L
        val departmentId = 1L
        val userId = 1L
        
        val department = Department(
            id = departmentId,
            name = "대학부"
        )
        
        val village = Village(
            id = villageId,
            name = "1마을",
            department = department
        )
        
        val villageLeader = mockk<VillageLeader>()
        val leaderUser = mockk<User>()
        
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) } returns villageLeader
        every { villageLeader.user } returns leaderUser
        every { leaderUser.id } returns userId
        every { authentication.principal } returns userDetailsAdapter
        every { userDetailsAdapter.getUser() } returns user
        every { user.id } returns userId
        
        // when
        val result = villageService.getVillageById(villageId, authentication)
        
        // then
        assertEquals(villageId, result.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(departmentId, result.departmentId)
        assertEquals("대학부", result.departmentName)
        assertTrue(result.isVillageLeader)
        
        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { villageLeaderRepository.findByVillageIdAndEndDateIsNull(villageId) }
        verify(exactly = 1) { userDetailsAdapter.getUser() }
    }
    
    @Test
    fun `존재하지 않는 마을 ID로 조회하면 예외가 발생한다`() {
        // given
        val villageId = 999L
        
        every { villageRepository.findById(villageId) } returns Optional.empty()
        
        // when & then
        assertThrows<AttendlyApiException> {
            villageService.getVillageById(villageId, authentication)
        }
        
        verify(exactly = 1) { villageRepository.findById(villageId) }
    }
} 