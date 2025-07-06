package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.GbsMemberSearchDto
import com.attendly.api.dto.LeaderGbsResponse
import com.attendly.api.dto.LeaderGbsHistoryListResponse
import com.attendly.api.dto.LeaderGbsHistoryRequestDto
import com.attendly.api.dto.GbsMemberResponse
import com.attendly.api.util.ResponseUtil
import com.attendly.domain.entity.*
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import com.attendly.service.GbsMemberService
import com.attendly.service.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import java.time.LocalDate

class GbsMemberControllerTest {

    private lateinit var gbsMemberService: GbsMemberService
    private lateinit var userService: UserService
    private lateinit var controller: GbsMemberController
    private lateinit var authentication: Authentication

    private val gbsId = 1L
    private val date = LocalDate.now()

    @BeforeEach
    fun setUp() {
        gbsMemberService = mockk()
        userService = mockk()
        controller = GbsMemberController(gbsMemberService, userService)
        authentication = UsernamePasswordAuthenticationToken("test@example.com", "password", emptyList())
    }

    @Test
    fun `관리자가 GBS 멤버를 조회할 수 있다`() {
        // Given
        val admin = User(
            id = 1L,
            name = "관리자",
            email = "admin@example.com",
            role = Role.ADMIN,
            status = UserStatus.ACTIVE,
            department = mockk()
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
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                ),
                GbsMemberResponse(
                    id = 2L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                )
            )
        )

        val searchDto = GbsMemberSearchDto(gbsId, date, admin)

        every { userService.getCurrentUser(authentication) } returns admin
        every { gbsMemberService.getGbsMembers(searchDto) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)

        verify(exactly = 1) { userService.getCurrentUser(authentication) }
        verify(exactly = 1) { gbsMemberService.getGbsMembers(searchDto) }
    }

    @Test
    fun `리더가 자신의 GBS 멤버를 조회할 수 있다`() {
        // Given
        val leader = User(
            id = 2L,
            name = "리더",
            email = "leader@example.com",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            department = mockk()
        )

        val response = GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS1",
            memberCount = 3,
            members = listOf(
                GbsMemberResponse(
                    id = 1L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                ),
                GbsMemberResponse(
                    id = 2L,
                    name = "조원2",
                    email = "member2@example.com",
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                ),
                GbsMemberResponse(
                    id = 3L,
                    name = "조원3",
                    email = "member3@example.com",
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                )
            )
        )

        val searchDto = GbsMemberSearchDto(gbsId, date, leader)

        every { userService.getCurrentUser(authentication) } returns leader
        every { gbsMemberService.getGbsMembers(searchDto) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)

        verify(exactly = 1) { userService.getCurrentUser(authentication) }
        verify(exactly = 1) { gbsMemberService.getGbsMembers(searchDto) }
    }

    @Test
    fun `위임받은 리더가 해당 GBS 멤버를 조회할 수 있다`() {
        // Given
        val delegatedLeader = User(
            id = 3L,
            name = "위임받은리더",
            email = "delegated@example.com",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            department = mockk()
        )

        val response = GbsMembersListResponse(
            gbsId = gbsId,
            gbsName = "GBS1",
            memberCount = 1,
            members = listOf(
                GbsMemberResponse(
                    id = 1L,
                    name = "조원1",
                    email = "member1@example.com",
                    birthDate = null,
                    joinDate = LocalDate.now(),
                    phoneNumber = null
                )
            )
        )

        val searchDto = GbsMemberSearchDto(gbsId, date, delegatedLeader)

        every { userService.getCurrentUser(authentication) } returns delegatedLeader
        every { gbsMemberService.getGbsMembers(searchDto) } returns response

        // When
        val result = controller.getGbsMembers(gbsId, date, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)

        verify(exactly = 1) { userService.getCurrentUser(authentication) }
        verify(exactly = 1) { gbsMemberService.getGbsMembers(searchDto) }
    }

    @Test
    fun `리더가 자신의 GBS 정보를 조회할 수 있다`() {
        // Given
        val leader = User(
            id = 2L,
            name = "리더",
            email = "leader@example.com",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            department = mockk()
        )

        val response = LeaderGbsResponse(
            gbsId = 1L,
            gbsName = "GBS1",
            villageId = 1L,
            villageName = "1마을",
            leaderId = 2L,
            leaderName = "리더",
            startDate = LocalDate.now()
        )

        every { userService.getCurrentUser(authentication) } returns leader
        every { gbsMemberService.getGbsForLeader(2L) } returns response

        // When
        val result = controller.getMyGbs(authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)

        verify(exactly = 1) { userService.getCurrentUser(authentication) }
        verify(exactly = 1) { gbsMemberService.getGbsForLeader(2L) }
    }

    @Test
    fun `리더의 GBS 히스토리를 조회할 수 있다`() {
        // Given
        val leader = User(
            id = 2L,
            name = "리더",
            email = "leader@example.com",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            department = mockk()
        )

        val response = LeaderGbsHistoryListResponse(
            leaderId = 2L,
            leaderName = "리더",
            historyCount = 1,
            histories = emptyList()
        )

        val request = LeaderGbsHistoryRequestDto(2L, leader)

        every { userService.getCurrentUser(authentication) } returns leader
        every { gbsMemberService.getLeaderGbsHistories(request) } returns response

        // When
        val result = controller.getLeaderGbsHistories(2L, authentication)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(response, result.body?.data)

        verify(exactly = 1) { userService.getCurrentUser(authentication) }
        verify(exactly = 1) { gbsMemberService.getLeaderGbsHistories(request) }
    }
} 