package com.attendly.api.controller.admin

import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.VillageResponse
import com.attendly.service.AdminOrganizationService
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class AdminOrganizationControllerTest {

    @MockK
    private lateinit var adminOrganizationService: AdminOrganizationService

    @InjectMockKs
    private lateinit var adminOrganizationController: AdminOrganizationController

    private fun setupMockMvc() = MockMvcBuilders.standaloneSetup(adminOrganizationController).build()

    @Test
    @DisplayName("마을 목록 조회 - 조건 없음")
    fun getAllVillagesWithoutFilters() {
        // given
        val now = LocalDateTime.now()
        val villageResponses = listOf(
            VillageResponse(
                id = 1L,
                name = "동문마을",
                departmentId = 1L,
                departmentName = "청년부",
                villageLeaderId = 101L,
                villageLeaderName = "김철수",
                createdAt = now,
                updatedAt = now
            ),
            VillageResponse(
                id = 2L,
                name = "서문마을",
                departmentId = 1L,
                departmentName = "청년부",
                villageLeaderId = 102L,
                villageLeaderName = "이영희",
                createdAt = now,
                updatedAt = now
            )
        )
        
        val pageResponse = PageResponse(
            items = villageResponses,
            totalCount = 2L,
            hasMore = false
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        
        every { 
            adminOrganizationService.getAllVillages(null, null, pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/villages"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].name").value("동문마을"))
            .andExpect(jsonPath("$.data.items[1].name").value("서문마을"))
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.hasMore").value(false))
            .andExpect(jsonPath("$.message").value("마을 목록 조회 성공"))
            .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
        
        verify(exactly = 1) { adminOrganizationService.getAllVillages(null, null, pageable) }
    }
    
    @Test
    @DisplayName("마을 목록 조회 - 부서 ID 필터링")
    fun getAllVillagesWithDepartmentFilter() {
        // given
        val departmentId = 1L
        val now = LocalDateTime.now()
        val villageResponses = listOf(
            VillageResponse(
                id = 1L,
                name = "동문마을",
                departmentId = departmentId,
                departmentName = "청년부",
                villageLeaderId = 101L,
                villageLeaderName = "김철수",
                createdAt = now,
                updatedAt = now
            )
        )
        
        val pageResponse = PageResponse(
            items = villageResponses,
            totalCount = 1L,
            hasMore = false
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        
        every { 
            adminOrganizationService.getAllVillages(departmentId, null, pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/villages")
                .param("departmentId", departmentId.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].name").value("동문마을"))
            .andExpect(jsonPath("$.data.items[0].departmentId").value(departmentId))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.message").value("마을 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllVillages(departmentId, null, pageable) }
    }
    
    @Test
    @DisplayName("마을 목록 조회 - 이름 필터링")
    fun getAllVillagesWithNameFilter() {
        // given
        val name = "동문"
        val now = LocalDateTime.now()
        val villageResponses = listOf(
            VillageResponse(
                id = 1L,
                name = "동문마을",
                departmentId = 1L,
                departmentName = "청년부",
                villageLeaderId = 101L,
                villageLeaderName = "김철수",
                createdAt = now,
                updatedAt = now
            )
        )
        
        val pageResponse = PageResponse(
            items = villageResponses,
            totalCount = 1L,
            hasMore = false
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("name").ascending())
        
        every { 
            adminOrganizationService.getAllVillages(null, name, pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/villages")
                .param("name", name))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].name").value("동문마을"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.message").value("마을 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllVillages(null, name, pageable) }
    }
    
    @Test
    @DisplayName("마을 목록 조회 - 페이징")
    fun getAllVillagesWithPaging() {
        // given
        val page = 1
        val size = 10
        val now = LocalDateTime.now()
        val villageResponses = listOf(
            VillageResponse(
                id = 1L,
                name = "동문마을",
                departmentId = 1L,
                departmentName = "청년부",
                villageLeaderId = 101L,
                villageLeaderName = "김철수",
                createdAt = now,
                updatedAt = now
            )
        )
        
        val pageResponse = PageResponse(
            items = villageResponses,
            totalCount = 15L,
            hasMore = true
        )
        
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        
        every { 
            adminOrganizationService.getAllVillages(null, null, pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/villages")
                .param("page", page.toString())
                .param("size", size.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.totalCount").value(15))
            .andExpect(jsonPath("$.data.hasMore").value(true))
            .andExpect(jsonPath("$.message").value("마을 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllVillages(null, null, pageable) }
    }
} 