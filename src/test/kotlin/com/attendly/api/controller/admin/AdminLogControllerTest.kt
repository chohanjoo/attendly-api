package com.attendly.api.controller.admin

import com.attendly.api.dto.SystemLogResponseDto
import com.attendly.domain.entity.SystemLog
import com.attendly.security.JwtTokenProvider
import com.attendly.service.SystemLogService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.context.annotation.Import
import com.attendly.security.TestSecurityConfig

@WebMvcTest(AdminLogController::class)
@Import(TestSecurityConfig::class)
@DisplayName("관리자 로그 컨트롤러 테스트")
class AdminLogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var systemLogService: SystemLogService
    
    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    @MockkBean
    private lateinit var userDetailsService: UserDetailsService

    private val now = LocalDateTime.now()
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    @BeforeEach
    fun setup() {
        // 테스트 데이터 준비
        val log1 = SystemLog(
            id = 1L,
            level = "INFO",
            category = "APPLICATION",
            message = "테스트 로그 메시지 1",
            timestamp = now.minusDays(1),
            ipAddress = "127.0.0.1",
            additionalInfo = null,
            userId = null,
            userAgent = null,
            serverInstance = null
        )
        
        val log2 = SystemLog(
            id = 2L,
            level = "ERROR",
            category = "SECURITY",
            message = "테스트 로그 메시지 2",
            timestamp = now,
            ipAddress = "127.0.0.1",
            additionalInfo = null,
            userId = null,
            userAgent = null,
            serverInstance = null
        )
        
        val logs = listOf(log1, log2)
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(logs, pageable, logs.size.toLong())
        
        // Response DTO 객체를 직접 생성
        val logResponseDto1 = SystemLogResponseDto(
            id = 1L,
            level = "INFO",
            category = "APPLICATION", 
            message = "테스트 로그 메시지 1",
            timestamp = now.minusDays(1),
            ipAddress = "127.0.0.1",
            additionalInfo = null,
            userId = null,
            userAgent = null,
            serverInstance = null
        )
        
        val logResponseDto2 = SystemLogResponseDto(
            id = 2L,
            level = "ERROR",
            category = "SECURITY",
            message = "테스트 로그 메시지 2",
            timestamp = now,
            ipAddress = "127.0.0.1",
            additionalInfo = null,
            userId = null,
            userAgent = null,
            serverInstance = null
        )
        
        // Mock 설정 - SystemLogService
        every { systemLogService.getLogs(any(), any(), any(), any(), any(), any(), any()) } returns page
        every { systemLogService.getLogById(1L) } returns log1
        every { systemLogService.getLogById(999L) } returns null
        every { systemLogService.getLogCategories() } returns listOf("APPLICATION", "SECURITY")
        
        // Companion object 모킹 설정
        mockkObject(SystemLogResponseDto.Companion)
        every { SystemLogResponseDto.from(log1) } returns logResponseDto1
        every { SystemLogResponseDto.from(log2) } returns logResponseDto2
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("모든 로그 조회")
    fun testGetLogs() {
        mockMvc.perform(
            get("/api/admin/logs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].level").value("INFO"))
            .andExpect(jsonPath("$.content[1].level").value("ERROR"))
            
        verify { systemLogService.getLogs(null, null, null, null, null, null, any()) }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("레벨별 로그 조회")
    fun testGetLogsByLevel() {
        mockMvc.perform(
            get("/api/admin/logs")
                .param("level", "ERROR")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            
        verify { systemLogService.getLogs("ERROR", null, null, null, null, null, any()) }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("ID로 로그 조회 - 성공")
    fun testGetLogByIdSuccess() {
        mockMvc.perform(
            get("/api/admin/logs/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.level").value("INFO"))
            
        verify { systemLogService.getLogById(1L) }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("ID로 로그 조회 - 실패")
    fun testGetLogByIdNotFound() {
        mockMvc.perform(
            get("/api/admin/logs/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            
        verify { systemLogService.getLogById(999L) }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("로그 카테고리 목록 조회")
    fun testGetLogCategories() {
        mockMvc.perform(
            get("/api/admin/logs/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0]").value("APPLICATION"))
            .andExpect(jsonPath("$[1]").value("SECURITY"))
            
        verify { systemLogService.getLogCategories() }
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    @DisplayName("로그 레벨 목록 조회")
    fun testGetLogLevels() {
        mockMvc.perform(
            get("/api/admin/logs/levels")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0]").value("INFO"))
            .andExpect(jsonPath("$[1]").value("WARN"))
            .andExpect(jsonPath("$[2]").value("ERROR"))
            
        // getLevels는 하드코딩된 값이므로 서비스 호출이 없음
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"])
    @DisplayName("권한 없는 사용자 접근 테스트")
    fun testUnauthorizedAccess() {
        mockMvc.perform(
            get("/api/admin/logs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }
} 