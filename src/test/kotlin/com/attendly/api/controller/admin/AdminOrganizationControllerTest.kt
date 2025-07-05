package com.attendly.api.controller.admin

import com.attendly.api.dto.*
import com.attendly.service.AdminOrganizationService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDate
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
    
    @Test
    @DisplayName("GBS 그룹 목록 조회 - 성공")
    fun getAllGbsGroups_Success() {
        // given
        val now = LocalDateTime.now()
        val gbsResponses = listOf(
            AdminGbsGroupListResponse(
                id = 1L,
                name = "믿음 GBS",
                villageId = 1L,
                villageName = "1마을",
                termStartDate = LocalDate.of(2024, 1, 1),
                termEndDate = LocalDate.of(2024, 6, 30),
                leaderId = 101L,
                leaderName = "김리더",
                createdAt = now,
                updatedAt = now,
                memberCount = 5
            ),
            AdminGbsGroupListResponse(
                id = 2L,
                name = "소망 GBS",
                villageId = 1L,
                villageName = "1마을",
                termStartDate = LocalDate.of(2024, 1, 1),
                termEndDate = LocalDate.of(2024, 6, 30),
                leaderId = null,
                leaderName = null,
                createdAt = now,
                updatedAt = now,
                memberCount = 3
            )
        )
        
        val pageResponse = PageResponse(
            items = gbsResponses,
            totalCount = 2L,
            hasMore = false
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("id").descending())
        
        every { 
            adminOrganizationService.getAllGbsGroups(pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/gbs-groups"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].id").value(1))
            .andExpect(jsonPath("$.data.items[0].name").value("믿음 GBS"))
            .andExpect(jsonPath("$.data.items[0].villageId").value(1))
            .andExpect(jsonPath("$.data.items[0].villageName").value("1마을"))
            .andExpect(jsonPath("$.data.items[0].leaderId").value(101))
            .andExpect(jsonPath("$.data.items[0].leaderName").value("김리더"))
            .andExpect(jsonPath("$.data.items[0].memberCount").value(5))
            .andExpect(jsonPath("$.data.items[1].id").value(2))
            .andExpect(jsonPath("$.data.items[1].name").value("소망 GBS"))
            .andExpect(jsonPath("$.data.items[1].leaderId").isEmpty)
            .andExpect(jsonPath("$.data.items[1].leaderName").isEmpty)
            .andExpect(jsonPath("$.data.items[1].memberCount").value(3))
            .andExpect(jsonPath("$.data.totalCount").value(2))
            .andExpect(jsonPath("$.data.hasMore").value(false))
            .andExpect(jsonPath("$.message").value("GBS 그룹 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllGbsGroups(pageable) }
    }
    
    @Test
    @DisplayName("GBS 그룹 목록 조회 - 페이징")
    fun getAllGbsGroups_WithPaging() {
        // given
        val page = 1
        val size = 10
        val now = LocalDateTime.now()
        val gbsResponses = listOf(
            AdminGbsGroupListResponse(
                id = 1L,
                name = "믿음 GBS",
                villageId = 1L,
                villageName = "1마을",
                termStartDate = LocalDate.of(2024, 1, 1),
                termEndDate = LocalDate.of(2024, 6, 30),
                leaderId = 101L,
                leaderName = "김리더",
                createdAt = now,
                updatedAt = now,
                memberCount = 5
            )
        )
        
        val pageResponse = PageResponse(
            items = gbsResponses,
            totalCount = 25L,
            hasMore = true
        )
        
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        
        every { 
            adminOrganizationService.getAllGbsGroups(pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/gbs-groups")
                .param("page", page.toString())
                .param("size", size.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.totalCount").value(25))
            .andExpect(jsonPath("$.data.hasMore").value(true))
            .andExpect(jsonPath("$.message").value("GBS 그룹 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllGbsGroups(pageable) }
    }
    
    @Test
    @DisplayName("GBS 그룹 목록 조회 - 빈 결과")
    fun getAllGbsGroups_EmptyResult() {
        // given
        val pageResponse = PageResponse<AdminGbsGroupListResponse>(
            items = emptyList(),
            totalCount = 0L,
            hasMore = false
        )
        
        val pageable = PageRequest.of(0, 20, Sort.by("id").descending())
        
        every { 
            adminOrganizationService.getAllGbsGroups(pageable) 
        } returns pageResponse
        
        val mockMvc = setupMockMvc()
        
        // when & then
        mockMvc.perform(get("/api/admin/organization/gbs-groups"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items.length()").value(0))
            .andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.hasMore").value(false))
            .andExpect(jsonPath("$.message").value("GBS 그룹 목록 조회 성공"))
        
        verify(exactly = 1) { adminOrganizationService.getAllGbsGroups(pageable) }
    }

    // 새로 추가된 API 테스트들
    @Test
    @DisplayName("GBS 그룹 멤버 조회 - 성공")
    fun getGbsGroupMembers_Success() {
        // given
        val gbsGroupId = 1L
        val memberResponses = listOf(
            GbsMemberResponse(
                id = 1L,
                name = "홍길동",
                email = "hong@test.com",
                birthDate = LocalDate.of(1995, 1, 1),
                joinDate = LocalDate.of(2024, 1, 1),
                phoneNumber = "010-1234-5678"
            ),
            GbsMemberResponse(
                id = 2L,
                name = "김철수",
                email = "kim@test.com",
                birthDate = LocalDate.of(1996, 2, 2),
                joinDate = LocalDate.of(2024, 1, 1),
                phoneNumber = "010-2345-6789"
            )
        )

        every { adminOrganizationService.getGbsGroupMembers(gbsGroupId) } returns memberResponses

        val mockMvc = setupMockMvc()

        // when & then
        mockMvc.perform(get("/api/admin/organization/gbs-groups/{gbsGroupId}/members", gbsGroupId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].name").value("홍길동"))
            .andExpect(jsonPath("$.data[0].email").value("hong@test.com"))
            .andExpect(jsonPath("$.data[0].phoneNumber").value("010-1234-5678"))
            .andExpect(jsonPath("$.data[1].id").value(2))
            .andExpect(jsonPath("$.data[1].name").value("김철수"))
            .andExpect(jsonPath("$.message").value("GBS 그룹 멤버 조회 성공"))

        verify(exactly = 1) { adminOrganizationService.getGbsGroupMembers(gbsGroupId) }
    }

    @Test
    @DisplayName("GBS 그룹 멤버 조회 - 빈 결과")
    fun getGbsGroupMembers_EmptyResult() {
        // given
        val gbsGroupId = 1L
        val memberResponses = emptyList<GbsMemberResponse>()

        every { adminOrganizationService.getGbsGroupMembers(gbsGroupId) } returns memberResponses

        val mockMvc = setupMockMvc()

        // when & then
        mockMvc.perform(get("/api/admin/organization/gbs-groups/{gbsGroupId}/members", gbsGroupId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(0))
            .andExpect(jsonPath("$.message").value("GBS 그룹 멤버 조회 성공"))

        verify(exactly = 1) { adminOrganizationService.getGbsGroupMembers(gbsGroupId) }
    }

    @Test
    @DisplayName("GBS 그룹 삭제 - 성공")
    fun deleteGbsGroup_Success() {
        // given
        val gbsGroupId = 1L

        justRun { adminOrganizationService.deleteGbsGroup(gbsGroupId) }

        val mockMvc = setupMockMvc()

        // when & then
        mockMvc.perform(delete("/api/admin/organization/gbs-groups/{gbsGroupId}", gbsGroupId))
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.message").value("GBS 그룹이 성공적으로 삭제되었습니다"))

        verify(exactly = 1) { adminOrganizationService.deleteGbsGroup(gbsGroupId) }
    }

    @Test
    @DisplayName("GBS 리더 해제 - 성공 (요청 본문 없음)")
    fun removeLeaderFromGbs_Success_NoRequestBody() {
        // given
        val gbsGroupId = 1L
        val request = GbsLeaderRemoveRequest()

        justRun { adminOrganizationService.removeLeaderFromGbs(gbsGroupId, request) }

        val mockMvc = setupMockMvc()

        // when & then
        mockMvc.perform(delete("/api/admin/organization/gbs-groups/{gbsGroupId}/leaders", gbsGroupId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.message").value("리더가 성공적으로 해제되었습니다"))

        verify(exactly = 1) { adminOrganizationService.removeLeaderFromGbs(gbsGroupId, any()) }
    }

    @Test
    @DisplayName("GBS 리더 해제 - 성공 (종료일 지정)")
    fun removeLeaderFromGbs_Success_WithEndDate() {
        // given
        val gbsGroupId = 1L
        val endDate = LocalDate.of(2024, 6, 30)
        val request = GbsLeaderRemoveRequest(endDate = endDate)

        justRun { adminOrganizationService.removeLeaderFromGbs(gbsGroupId, request) }

        val mockMvc = setupMockMvc()
        val objectMapper = ObjectMapper().apply {
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
        }

        // when & then
        mockMvc.perform(delete("/api/admin/organization/gbs-groups/{gbsGroupId}/leaders", gbsGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
            .andExpect(jsonPath("$.message").value("리더가 성공적으로 해제되었습니다"))

        verify(exactly = 1) { adminOrganizationService.removeLeaderFromGbs(gbsGroupId, any()) }
    }
} 