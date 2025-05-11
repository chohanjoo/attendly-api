package com.attendly.api.controller

import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.LeaderGbsHistoryListResponse
import com.attendly.api.dto.LeaderGbsHistoryResponse
import com.attendly.api.dto.LeaderGbsResponse
import com.attendly.domain.entity.*
import com.attendly.enums.Role
import com.attendly.service.GbsMemberService
import com.attendly.service.UserService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class GbsMemberControllerTest {

    private lateinit var controller: GbsMemberController
    private lateinit var gbsMemberService: GbsMemberService
    private lateinit var userService: UserService
    private lateinit var authentication: Authentication

    private val gbsId = 1L
    private val date = LocalDate.now()
    private val userId = 1L

    @BeforeEach
    fun setup() {
        gbsMemberService = mockk()
        userService = mockk()
        authentication = mockk()
        
        controller = GbsMemberController(
            gbsMemberService,
            userService
        )
    }

    @Test
    fun `관리자는 모든 GBS 조원 정보를 조회할 수 있다`() {
        // Given
        val department = Department(id = 1L, name = "대학부")
        val admin = User(
            id = userId,
            name = "어드민",
            role = Role.ADMIN,
            email = "admin@example.com",
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            department = department
        )
        
        val response = GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS1",
            memberCount = 2,
            members = listOf(
                GbsMemberResponse(
                    id = 1L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = LocalDate.of(2000, 1, 1),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                ),
                GbsMemberResponse(
                    id = 2L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = LocalDate.of(2000, 2, 2),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                )
            )
        )

        every { userService.getCurrentUser(authentication) } returns admin
        every { gbsMemberService.getGbsMembers(gbsId, date, admin) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(2, result.body?.data?.memberCount)
        assertEquals(gbsId, result.body?.data?.gbsId)
    }

    @Test
    fun `리더는 자신의 GBS 조원 정보를 조회할 수 있다`() {
        // Given
        val department = Department(id = 1L, name = "대학부")
        val leader = User(
            id = userId,
            name = "리더",
            role = Role.LEADER,
            email = "leader@example.com",
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            department = department
        )
        
        val response = GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS1",
            memberCount = 2,
            members = listOf(
                GbsMemberResponse(
                    id = 2L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = LocalDate.of(2000, 1, 1),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                ),
                GbsMemberResponse(
                    id = 3L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = LocalDate.of(2000, 2, 2),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                )
            )
        )

        every { userService.getCurrentUser(authentication) } returns leader
        every { gbsMemberService.getGbsMembers(gbsId, date, leader) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(2, result.body?.data?.memberCount)
        assertEquals(gbsId, result.body?.data?.gbsId)
    }

    @Test
    fun `위임받은 리더는 해당 GBS 조원 정보를 조회할 수 있다`() {
        // Given
        val department = Department(id = 1L, name = "대학부")
        val delegatedLeader = User(
            id = userId,
            name = "위임받은리더",
            role = Role.LEADER,
            email = "delegated@example.com",
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            department = department
        )
        
        val response = GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS1",
            memberCount = 2,
            members = listOf(
                GbsMemberResponse(
                    id = 3L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = LocalDate.of(2000, 1, 1),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                ),
                GbsMemberResponse(
                    id = 4L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = LocalDate.of(2000, 2, 2),
                    joinDate = LocalDate.of(2023, 1, 1),
                    phoneNumber = "010-1234-5678"
                )
            )
        )

        every { userService.getCurrentUser(authentication) } returns delegatedLeader
        every { gbsMemberService.getGbsMembers(gbsId, date, delegatedLeader) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(2, result.body?.data?.memberCount)
        assertEquals(gbsId, result.body?.data?.gbsId)
    }

    @Test
    fun `리더의 GBS 조회 API는 유효한 데이터를 반환한다`() {
        // given
        val department = Department(id = 1L, name = "대학부")
        val user = User(
            id = userId, 
            name = "홍길동", 
            email = "test@example.com", 
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            role = Role.LEADER,
            department = department
        )
        
        val expectedResponse = LeaderGbsResponse(
            gbsId = 1L,
            gbsName = "Test GBS",
            villageId = 1L,
            villageName = "Test Village",
            leaderId = userId,
            leaderName = "홍길동",
            startDate = LocalDate.now()
        )

        // when
        every { userService.getCurrentUser(any()) } returns user
        every { gbsMemberService.getGbsForLeader(userId) } returns expectedResponse

        // then
        val response = controller.getMyGbs(authentication)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body?.success == true)
        assertEquals(expectedResponse, response.body?.data)
    }

    @Test
    fun `리더의 GBS 히스토리 조회 API는 유효한 데이터를 반환한다`() {
        // Given
        val leaderId = 1L
        val department = Department(id = 1L, name = "대학부")
        val leader = User(
            id = leaderId,
            name = "홍길동",
            role = Role.LEADER,
            email = "leader@example.com",
            password = "password",
            birthDate = LocalDate.of(1990, 1, 1),
            department = department
        )
        
        val members = listOf(
            GbsMemberResponse(
                id = 2L,
                name = "조원1",
                email = "member1@example.com",
                birthDate = LocalDate.of(2000, 1, 1),
                joinDate = LocalDate.of(2022, 1, 1),
                phoneNumber = "010-1234-5678"
            ),
            GbsMemberResponse(
                id = 3L,
                name = "조원2",
                email = "member2@example.com",
                birthDate = LocalDate.of(2000, 2, 2),
                joinDate = LocalDate.of(2022, 1, 1),
                phoneNumber = "010-1234-5678"
            )
        )
        
        val historyResponse = LeaderGbsHistoryListResponse(
            leaderId = leaderId,
            leaderName = "홍길동",
            historyCount = 2,
            histories = listOf(
                LeaderGbsHistoryResponse(
                    historyId = 2L,
                    gbsId = 2L,
                    gbsName = "GBS2",
                    villageId = 1L,
                    villageName = "1마을",
                    startDate = LocalDate.of(2023, 1, 1),
                    endDate = null,
                    isActive = true,
                    members = members
                ),
                LeaderGbsHistoryResponse(
                    historyId = 1L,
                    gbsId = 1L,
                    gbsName = "GBS1",
                    villageId = 1L,
                    villageName = "1마을",
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2022, 12, 31),
                    isActive = false,
                    members = members
                )
            )
        )
        
        every { userService.getCurrentUser(authentication) } returns leader
        every { gbsMemberService.getLeaderGbsHistories(leaderId, leader) } returns historyResponse
        
        // When
        val result = controller.getLeaderGbsHistories(leaderId, authentication)
        
        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(leaderId, result.body?.data?.leaderId)
        assertEquals("홍길동", result.body?.data?.leaderName)
        assertEquals(2, result.body?.data?.historyCount)
        assertEquals(2, result.body?.data?.histories?.size)
        
        // 첫 번째 히스토리 검증 (현재 활성 GBS)
        val firstHistory = result.body?.data?.histories?.get(0)
        assertEquals(2L, firstHistory?.historyId)
        assertEquals(true, firstHistory?.isActive)
        
        // 두 번째 히스토리 검증 (과거 GBS)
        val secondHistory = result.body?.data?.histories?.get(1)
        assertEquals(1L, secondHistory?.historyId)
        assertEquals(false, secondHistory?.isActive)
        assertEquals(LocalDate.of(2022, 12, 31), secondHistory?.endDate)
    }
    
    @Test
    fun `관리자는 다른 리더의 GBS 히스토리를 조회할 수 있다`() {
        // Given
        val leaderId = 1L
        val adminId = 99L
        val department = Department(id = 1L, name = "대학부")
        
        val admin = User(
            id = adminId,
            name = "관리자",
            role = Role.ADMIN,
            email = "admin@example.com",
            password = "password",
            birthDate = LocalDate.of(1985, 1, 1),
            department = department
        )
        
        val historyResponse = LeaderGbsHistoryListResponse(
            leaderId = leaderId,
            leaderName = "홍길동",
            historyCount = 1,
            histories = listOf(
                LeaderGbsHistoryResponse(
                    historyId = 1L,
                    gbsId = 1L,
                    gbsName = "GBS1",
                    villageId = 1L,
                    villageName = "1마을",
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2022, 12, 31),
                    isActive = false,
                    members = listOf()
                )
            )
        )
        
        every { userService.getCurrentUser(authentication) } returns admin
        every { gbsMemberService.getLeaderGbsHistories(leaderId, admin) } returns historyResponse
        
        // When
        val result = controller.getLeaderGbsHistories(leaderId, authentication)
        
        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(leaderId, result.body?.data?.leaderId)
        assertEquals("홍길동", result.body?.data?.leaderName)
        assertEquals(1, result.body?.data?.historyCount)
    }
} 