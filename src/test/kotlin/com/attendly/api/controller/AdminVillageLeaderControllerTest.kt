package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.VillageLeaderAssignRequest
import com.attendly.api.dto.VillageLeaderResponse
import com.attendly.service.AdminVillageLeaderService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class AdminVillageLeaderControllerTest {

    private lateinit var controller: AdminVillageLeaderController
    private lateinit var adminVillageLeaderService: AdminVillageLeaderService

    @BeforeEach
    fun setup() {
        adminVillageLeaderService = mockk()
        controller = AdminVillageLeaderController(adminVillageLeaderService)
    }

    @Test
    fun `마을장 등록 성공`() {
        // given
        val request = VillageLeaderAssignRequest(
            userId = 1L,
            villageId = 1L,
            startDate = LocalDate.now()
        )

        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now(),
            endDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.assignVillageLeader(any()) } returns response

        // when
        val result = controller.assignVillageLeader(request)

        // then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(1L, result.body?.data?.userId)
        assertEquals("홍길동", result.body?.data?.userName)
        assertEquals(1L, result.body?.data?.villageId)
        assertEquals("마을1", result.body?.data?.villageName)
    }

    @Test
    fun `마을장 조회 성공`() {
        // given
        val villageId = 1L
        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now(),
            endDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.getVillageLeader(1L) } returns response

        // when
        val result = controller.getVillageLeader(villageId)

        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(1L, result.body?.data?.userId)
        assertEquals("홍길동", result.body?.data?.userName)
        assertEquals(1L, result.body?.data?.villageId)
        assertEquals("마을1", result.body?.data?.villageName)
    }

    @Test
    fun `마을장이 없는 경우 404 반환`() {
        // given
        val villageId = 1L
        every { adminVillageLeaderService.getVillageLeader(1L) } returns null

        // when
        val result = controller.getVillageLeader(villageId)

        // then
        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == false)
    }

    @Test
    fun `마을장 종료 성공`() {
        // given
        val villageId = 1L
        val endDate = LocalDate.now()

        val response = VillageLeaderResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 1L,
            villageName = "마을1",
            startDate = LocalDate.now().minusDays(30),
            endDate = endDate,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { adminVillageLeaderService.terminateVillageLeader(1L, endDate) } returns response

        // when
        val result = controller.terminateVillageLeader(villageId, endDate)

        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        assertEquals(1L, result.body?.data?.userId)
        assertNotNull(result.body?.data?.endDate)
    }
} 