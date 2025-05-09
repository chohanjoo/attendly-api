package com.attendly.service

import com.attendly.api.dto.VillageLeaderAssignRequest
import com.attendly.domain.entity.*
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageLeaderRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class AdminVillageLeaderServiceTest {

    @MockK
    lateinit var villageLeaderRepository: VillageLeaderRepository

    @MockK
    lateinit var userRepository: UserRepository

    @MockK
    lateinit var villageRepository: VillageRepository

    @InjectMockKs
    lateinit var adminVillageLeaderService: AdminVillageLeaderService

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var user: User
    private lateinit var villageLeader: VillageLeader

    @BeforeEach
    fun setUp() {
        department = Department(
            id = 1L,
            name = "교구"
        )

        village = Village(
            id = 1L,
            name = "마을1",
            department = department
        )

        user = User(
            id = 1L,
            name = "홍길동",
            role = Role.VILLAGE_LEADER,
            department = department
        )

        villageLeader = VillageLeader(
            user = user,
            village = village,
            startDate = LocalDate.now().minusDays(30),
        )
    }

    @Test
    fun `마을장 등록 성공`() {
        // given
        val request = VillageLeaderAssignRequest(
            userId = 1L,
            villageId = 1L,
            startDate = LocalDate.now()
        )

        every { userRepository.findById(1L) } returns Optional.of(user)
        every { villageRepository.findById(1L) } returns Optional.of(village)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns null
        every { villageLeaderRepository.findByUserIdAndEndDateIsNull(1L) } returns null
        every { villageLeaderRepository.save(any()) } returns villageLeader

        // when
        val result = adminVillageLeaderService.assignVillageLeader(request)

        // then
        assertNotNull(result)
        assertEquals(user.id, result.userId)
        assertEquals(village.id, result.villageId)
        verify { villageLeaderRepository.save(any()) }
    }

    @Test
    fun `마을장 등록 시 이전 마을장 종료 처리`() {
        // given
        val request = VillageLeaderAssignRequest(
            userId = 2L,
            villageId = 1L,
            startDate = LocalDate.now()
        )

        val newUser = User(
            id = 2L,
            name = "김철수",
            role = Role.VILLAGE_LEADER,
            department = department
        )
        
        every { userRepository.findById(2L) } returns Optional.of(newUser)
        every { villageRepository.findById(1L) } returns Optional.of(village)
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns villageLeader
        every { villageLeaderRepository.findByUserIdAndEndDateIsNull(2L) } returns null
        every { villageLeaderRepository.save(any()) } returns 
            VillageLeader(
                user = newUser,
                village = village,
                startDate = LocalDate.now(),
            )

        // when
        val result = adminVillageLeaderService.assignVillageLeader(request)

        // then
        assertNotNull(result)
        assertEquals(newUser.id, result.userId)
        verify { villageLeaderRepository.save(any()) } // 이전 마을장 종료 저장
        verify { villageLeaderRepository.save(any()) } // 새 마을장 저장
    }

    @Test
    fun `마을장 종료 성공`() {
        // given
        val endDate = LocalDate.now()

        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns villageLeader
        every { villageLeaderRepository.save(any()) } returns 
            VillageLeader(
                user = user,
                village = village,
                startDate = villageLeader.startDate,
                endDate = endDate
            )

        // when
        val result = adminVillageLeaderService.terminateVillageLeader(1L, endDate)

        // then
        assertNotNull(result)
        assertEquals(endDate, result.endDate)
        verify { villageLeaderRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 마을장 종료 시 예외 발생`() {
        // given
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns null

        // when & then
        assertThrows<AttendlyApiException> {
            adminVillageLeaderService.terminateVillageLeader(1L, LocalDate.now())
        }
    }

    @Test
    fun `마을장 조회 성공`() {
        // given
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns villageLeader

        // when
        val result = adminVillageLeaderService.getVillageLeader(1L)

        // then
        assertNotNull(result)
        assertEquals(user.id, result?.userId)
        assertEquals(village.id, result?.villageId)
    }

    @Test
    fun `마을장이 없는 경우 null 반환`() {
        // given
        every { villageLeaderRepository.findByVillageIdAndEndDateIsNull(1L) } returns null

        // when
        val result = adminVillageLeaderService.getVillageLeader(1L)

        // then
        assertNull(result)
    }
} 