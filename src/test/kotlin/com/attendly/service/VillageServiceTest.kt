package com.attendly.service

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.model.UserFilterDto
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.security.UserDetailsAdapter
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.Authentication
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class VillageServiceTest {

    private lateinit var villageService: VillageService
    private lateinit var villageRepository: VillageRepository
    private lateinit var villageLeaderRepository: VillageLeaderRepository
    private lateinit var authentication: Authentication
    private lateinit var userDetailsAdapter: UserDetailsAdapter
    private lateinit var user: User
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        villageRepository = mockk()
        villageLeaderRepository = mockk()
        authentication = mockk()
        userDetailsAdapter = mockk()
        user = mockk()
        userRepository = mockk()

        villageService = VillageService(
            villageRepository = villageRepository,
            villageLeaderRepository = villageLeaderRepository,
            userRepository = userRepository
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

    @Test
    fun `마을 멤버 목록 조회 성공`() {
        // Given
        val villageId = 1L
        val department = Department(
            id = 1L,
            name = "청년부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val village = Village(
            id = villageId,
            name = "1마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val users = listOf(
            User(
                id = 1L,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                role = Role.MEMBER,
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                department = department,
                village = village,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            User(
                id = 2L,
                name = "김철수",
                birthDate = LocalDate.of(1992, 3, 15),
                role = Role.MEMBER,
                email = "kim@example.com",
                phoneNumber = "010-2345-6789",
                department = department,
                village = village,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        every { villageRepository.findById(villageId) } returns Optional.of(village)

        val userFilterDto = UserFilterDto(villageId = villageId, roles = listOf(Role.MEMBER))
        every { userRepository.findByFilters(userFilterDto) } returns users

        // When
        val result = villageService.getVillageMembers(villageId)

        // Then
        assertNotNull(result)

        with(result[0]) {
            assertEquals(1L, id)
            assertEquals("홍길동", name)
            assertEquals(LocalDate.of(1990, 1, 1), birthDate)
            assertEquals("hong@example.com", email)
            assertEquals("010-1234-5678", phoneNumber)
            assertEquals("MEMBER", role)
        }

        with(result[1]) {
            assertEquals(2L, id)
            assertEquals("김철수", name)
            assertEquals(LocalDate.of(1992, 3, 15), birthDate)
            assertEquals("kim@example.com", email)
            assertEquals("010-2345-6789", phoneNumber)
            assertEquals("MEMBER", role)
        }

        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { userRepository.findByFilters(userFilterDto) }
    }

    @Test
    fun `존재하지 않는 마을 ID로 멤버 목록 조회 시 예외 발생`() {
        // Given
        val nonExistentVillageId = 999L
        every { villageRepository.findById(nonExistentVillageId) } returns Optional.empty()

        // When & Then
        assertThrows<AttendlyApiException> {
            villageService.getVillageMembers(nonExistentVillageId)
        }

        verify(exactly = 1) { villageRepository.findById(nonExistentVillageId) }
        verify(exactly = 0) { userRepository.findByVillageId(any()) }
    }
} 