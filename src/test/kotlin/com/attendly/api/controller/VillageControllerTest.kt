package com.attendly.api.controller

import com.attendly.api.dto.*
import com.attendly.service.GbsGroupService
import com.attendly.service.GbsMemberService
import com.attendly.service.VillageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class VillageControllerTest {

    private lateinit var villageController: VillageController
    private lateinit var villageService: VillageService
    private lateinit var gbsMemberService: GbsMemberService
    private lateinit var gbsGroupService: GbsGroupService
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setUp() {
        villageService = mockk()
        gbsMemberService = mockk()
        gbsGroupService = mockk()
        authentication = mockk()
        villageController = VillageController(villageService, gbsMemberService, gbsGroupService)
    }

    @Test
    fun `특정 마을 정보를 조회한다`() {
        // given
        val villageId = 1L
        val expectedResponse = UserVillageResponse(
            userId = 0L,
            userName = "",
            villageId = villageId,
            villageName = "1마을",
            departmentId = 1L,
            departmentName = "대학부",
            isVillageLeader = false
        )

        every { villageService.getVillageById(villageId, authentication) } returns expectedResponse

        // when
        val responseEntity = villageController.getVillageById(villageId, authentication)

        // then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals("마을 정보 조회 성공", apiResponse.message)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(villageId, result!!.villageId)
        assertEquals("1마을", result.villageName)
        assertEquals(1L, result.departmentId)
        assertEquals("대학부", result.departmentName)
        assertEquals(false, result.isVillageLeader)

        verify(exactly = 1) { villageService.getVillageById(villageId, authentication) }
    }

    @Test
    fun `GBS 배치 정보를 조회한다`() {
        // given
        val villageId = 1L
        val date = LocalDate.now()
        val columns = listOf(
            KanbanColumn(
                id = "unassigned",
                title = "미배정 멤버",
                cards = emptyList()
            )
        )
        val labels = listOf(
            KanbanLabel(
                id = 1L,
                name = "GBS1",
                color = "#FF5733"
            )
        )

        val expectedResponse = GbsAssignmentResponse(
            villageId = villageId,
            villageName = "1마을",
            columns = columns,
            labels = labels
        )

        every { gbsMemberService.getGbsAssignment(villageId, date) } returns expectedResponse

        // when
        val responseEntity = villageController.getGbsAssignment(villageId, date)

        // then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals("GBS 배치 정보 조회 성공", apiResponse.message)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(villageId, result!!.villageId)
        assertEquals("1마을", result.villageName)

        verify(exactly = 1) { gbsMemberService.getGbsAssignment(villageId, date) }
    }

    @Test
    fun `GBS 배치 정보를 저장한다`() {
        // given
        val villageId = 1L
        val request = GbsAssignmentSaveRequest(
            startDate = LocalDate.of(2023, 6, 1),
            endDate = LocalDate.of(2023, 12, 31),
            assignments = listOf(
                GbsAssignment(
                    gbsId = 1L,
                    leaderId = 10L,
                    memberIds = listOf(101L, 102L)
                ),
                GbsAssignment(
                    gbsId = 2L,
                    leaderId = 20L,
                    memberIds = listOf(201L)
                )
            )
        )

        val expectedResponse = GbsAssignmentSaveResponse(
            villageId = villageId,
            assignmentCount = 2,
            memberCount = 5,
            message = "GBS 배치가 성공적으로 저장되었습니다."
        )

        every { gbsMemberService.saveGbsAssignment(villageId, request) } returns expectedResponse

        // when
        val responseEntity = villageController.saveGbsAssignment(villageId, request)

        // then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals("GBS 배치 저장 성공", apiResponse.message)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(villageId, result!!.villageId)
        assertEquals(2, result.assignmentCount)
        assertEquals(5, result.memberCount)
        assertEquals("GBS 배치가 성공적으로 저장되었습니다.", result.message)

        verify(exactly = 1) { gbsMemberService.saveGbsAssignment(villageId, request) }
    }

    @Test
    fun `GBS 리더 후보 목록을 조회한다`() {
        // given
        val villageId = 1L
        val candidates = listOf(
            LeaderCandidate(
                id = 789L,
                name = "박지성",
                email = "park@example.com",
                isLeader = true,
                previousGbsCount = 2
            ),
            LeaderCandidate(
                id = 101L,
                name = "손흥민",
                email = "son@example.com",
                isLeader = true,
                previousGbsCount = 1
            )
        )

        every { gbsMemberService.getLeaderCandidates(villageId) } returns candidates

        // when
        val responseEntity = villageController.getLeaderCandidates(villageId)

        // then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(2, result!!.items.size)

        val firstCandidate = result.items[0]
        assertEquals(789L, firstCandidate.id)
        assertEquals("박지성", firstCandidate.name)
        assertEquals("park@example.com", firstCandidate.email)
        assertEquals(true, firstCandidate.isLeader)
        assertEquals(2, firstCandidate.previousGbsCount)

        val secondCandidate = result.items[1]
        assertEquals(101L, secondCandidate.id)
        assertEquals("손흥민", secondCandidate.name)
        assertEquals("son@example.com", secondCandidate.email)
        assertEquals(true, secondCandidate.isLeader)
        assertEquals(1, secondCandidate.previousGbsCount)

        verify(exactly = 1) { gbsMemberService.getLeaderCandidates(villageId) }
    }

    @Test
    @WithMockUser
    fun `getGbsGroups는 마을 ID로 GBS 그룹 목록을 조회한다`() {
        // given
        val villageId = 1L
        val gbsGroupResponse = GbsGroupListResponse(
            groups = listOf(
                GbsGroupInfo(
                    id = 1L,
                    name = "믿음 GBS",
                    description = "믿음 중심 GBS",
                    color = "#FF5733",
                    isActive = true
                ),
                GbsGroupInfo(
                    id = 2L,
                    name = "소망 GBS",
                    description = "소망 중심 GBS",
                    color = "#33FF57",
                    isActive = true
                )
            )
        )

        every { gbsGroupService.getGbsGroups(villageId) } returns gbsGroupResponse

        // when
        val responseEntity = villageController.getGbsGroups(villageId)

        // then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals("GBS 그룹 목록 조회 성공", apiResponse.message)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(2, result!!.groups.size)
        assertEquals("믿음 GBS", result.groups[0].name)
        assertEquals("소망 GBS", result.groups[1].name)

        verify(exactly = 1) { gbsGroupService.getGbsGroups(villageId) }
    }

    @Test
    fun `마을 멤버 목록 조회 성공`() {
        // Given
        val villageId = 1L
        val memberResponse = listOf(
            MemberInfo(
                id = 1L,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                role = "MEMBER",
                joinDate = LocalDate.of(2023, 1, 1)
            )
        )

        every { villageService.getVillageMembers(villageId) } returns memberResponse

        // When
        val responseEntity = villageController.getVillageMembers(villageId)

        // Then
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)

        val apiResponse = responseEntity.body
        assertNotNull(apiResponse)
        assertTrue(apiResponse!!.success)
        assertEquals(200, apiResponse.code)

        val result = apiResponse.data
        assertNotNull(result)
        assertEquals(1, result!!.totalCount)
        assertEquals(1L, result.items[0].id)
        assertEquals("홍길동", result.items[0].name)
        assertEquals("hong@example.com", result.items[0].email)
        assertEquals("MEMBER", result.items[0].role)

        verify(exactly = 1) { villageService.getVillageMembers(villageId) }
    }
} 