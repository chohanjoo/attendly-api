package com.attendly.api.controller

import com.attendly.api.dto.UserVillageResponse
import com.attendly.api.dto.GbsAssignmentResponse
import com.attendly.api.dto.GbsAssignmentSaveRequest
import com.attendly.api.dto.GbsAssignmentSaveResponse
import com.attendly.api.dto.GbsAssignment
import com.attendly.api.dto.KanbanColumn
import com.attendly.api.dto.KanbanLabel
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
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setUp() {
        villageService = mockk()
        gbsMemberService = mockk()
        authentication = mockk()
        villageController = VillageController(villageService, gbsMemberService)
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
} 