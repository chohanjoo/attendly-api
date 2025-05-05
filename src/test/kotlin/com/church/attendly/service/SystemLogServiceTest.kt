package com.church.attendly.service

import com.church.attendly.domain.entity.SystemLog
import com.church.attendly.domain.repository.SystemLogRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DisplayName("시스템 로그 서비스 테스트")
class SystemLogServiceTest {

    private lateinit var systemLogRepository: SystemLogRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var systemLogService: SystemLogService
    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication
    
    @BeforeEach
    fun setup() {
        systemLogRepository = mockk(relaxed = true)
        objectMapper = mockk(relaxed = true)
        securityContext = mockk<SecurityContext>()
        authentication = mockk<Authentication>()
        
        // SecurityContextHolder 설정
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
        
        // 서비스 인스턴스 생성
        systemLogService = SystemLogService(systemLogRepository, objectMapper)
    }
    
    @Test
    @DisplayName("로그 생성 테스트")
    fun testCreateLog() {
        // given
        val level = "INFO"
        val category = "TEST"
        val message = "테스트 로그 메시지"
        val request = mockk<HttpServletRequest>()
        
        // 인증 정보 Mock
        val userDetails = User("1", "password", emptyList())
        every { authentication.principal } returns userDetails
        every { authentication.isAuthenticated } returns true
        
        // request Mock
        every { request.remoteAddr } returns "127.0.0.1"
        every { request.getHeader("User-Agent") } returns "Test User Agent"
        
        // SystemLog 저장 Mock
        val savedSystemLog = SystemLog(
            id = 1L,
            level = level,
            category = category,
            message = message,
            ipAddress = "127.0.0.1",
            userId = 1L,
            userAgent = "Test User Agent"
        )
        val systemLogSlot = slot<SystemLog>()
        every { systemLogRepository.save(capture(systemLogSlot)) } returns savedSystemLog
        
        // when
        val result = systemLogService.createLog(level, category, message, null, request)
        
        // then
        verify { systemLogRepository.save(any()) }
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals(level, result.level)
        assertEquals(category, result.category)
        assertEquals(message, result.message)
        
        val capturedLog = systemLogSlot.captured
        assertEquals(level, capturedLog.level)
        assertEquals(category, capturedLog.category)
        assertEquals(message, capturedLog.message)
        assertEquals("127.0.0.1", capturedLog.ipAddress)
        assertEquals(1L, capturedLog.userId)
    }
    
    @Test
    @DisplayName("ID로 로그 조회 테스트")
    fun testGetLogById() {
        // given
        val logId = 1L
        val expectedLog = SystemLog(
            id = logId,
            level = "INFO",
            category = "TEST",
            message = "테스트 로그",
            timestamp = LocalDateTime.now()
        )
        
        every { systemLogRepository.findById(logId) } returns Optional.of(expectedLog)
        
        // when
        val result = systemLogService.getLogById(logId)
        
        // then
        verify { systemLogRepository.findById(logId) }
        assertNotNull(result)
        assertEquals(logId, result?.id)
    }
    
    @Test
    @DisplayName("로그 검색 테스트")
    fun testGetLogs() {
        // given
        val level = "INFO"
        val category = "TEST"
        val pageable = PageRequest.of(0, 10)
        val logs = listOf(
            SystemLog(
                id = 1L,
                level = level,
                category = category,
                message = "테스트 로그 1",
                timestamp = LocalDateTime.now()
            ),
            SystemLog(
                id = 2L,
                level = level,
                category = category,
                message = "테스트 로그 2",
                timestamp = LocalDateTime.now().minusDays(1)
            )
        )
        val page = PageImpl(logs, pageable, logs.size.toLong())
        
        // searchLogs 메서드 모킹
        every { 
            systemLogRepository.searchLogs(
                level, 
                category, 
                null, 
                null, 
                null, 
                null, 
                pageable
            ) 
        } returns page
        
        // when
        val result = systemLogService.getLogs(level, category, null, null, null, null, pageable)
        
        // then
        verify { 
            systemLogRepository.searchLogs(
                level, 
                category, 
                null, 
                null, 
                null, 
                null, 
                pageable
            ) 
        }
        assertEquals(2, result.content.size)
        assertEquals(1L, result.content[0].id)
        assertEquals(2L, result.content[1].id)
    }
    
    @Test
    @DisplayName("로그 카테고리 목록 조회 테스트")
    fun testGetLogCategories() {
        // when
        val categories = systemLogService.getLogCategories()
        
        // then
        assertEquals(8, categories.size)
        assert(categories.contains("APPLICATION"))
        assert(categories.contains("SECURITY"))
        assert(categories.contains("BATCH"))
        assert(categories.contains("AUDIT"))
        assert(categories.contains("ATTENDANCE"))
        assert(categories.contains("USER_MANAGEMENT"))
        assert(categories.contains("ORGANIZATION"))
        assert(categories.contains("API_CALL"))
    }
} 