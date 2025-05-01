package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.SystemLog
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@DataJpaTest
@DisplayName("시스템 로그 리포지토리 테스트")
@Import(TestQuerydslConfig::class, SystemLogRepositoryImpl::class)
class SystemLogRepositoryTest {

    @Autowired
    private lateinit var systemLogRepository: SystemLogRepository

    @Test
    @DisplayName("시스템 로그 저장 및 조회 테스트")
    fun testSaveAndFindSystemLog() {
        // given
        val now = LocalDateTime.now()
        val systemLog = SystemLog(
            level = "INFO",
            category = "TEST",
            message = "테스트 로그 메시지",
            timestamp = now,
            ipAddress = "127.0.0.1"
        )

        // when
        val savedLog = systemLogRepository.save(systemLog)

        // then
        assertNotNull(savedLog.id)
        
        // ID로 조회 테스트
        val foundLog = systemLogRepository.findById(savedLog.id!!).orElse(null)
        assertNotNull(foundLog)
        assertEquals("INFO", foundLog.level)
        assertEquals("TEST", foundLog.category)
        assertEquals("테스트 로그 메시지", foundLog.message)
    }

    @Test
    @DisplayName("레벨별 로그 조회 테스트")
    fun testFindByLevel() {
        // given
        val logs = mutableListOf<SystemLog>()
        for (i in 1..5) {
            logs.add(
                SystemLog(
                    level = "INFO",
                    category = "TEST",
                    message = "INFO 로그 $i",
                    timestamp = LocalDateTime.now()
                )
            )
        }
        
        for (i in 1..3) {
            logs.add(
                SystemLog(
                    level = "ERROR",
                    category = "TEST",
                    message = "ERROR 로그 $i",
                    timestamp = LocalDateTime.now()
                )
            )
        }
        
        systemLogRepository.saveAll(logs)
        
        // when
        val infoLogs = systemLogRepository.findByLevel("INFO", PageRequest.of(0, 10))
        val errorLogs = systemLogRepository.findByLevel("ERROR", PageRequest.of(0, 10))
        
        // then
        assertEquals(5, infoLogs.content.size)
        assertEquals(3, errorLogs.content.size)
    }
    
    @Test
    @DisplayName("카테고리별 로그 조회 테스트")
    fun testFindByCategory() {
        // given
        val logs = mutableListOf<SystemLog>()
        for (i in 1..3) {
            logs.add(
                SystemLog(
                    level = "INFO",
                    category = "APP",
                    message = "APP 로그 $i",
                    timestamp = LocalDateTime.now()
                )
            )
        }
        
        for (i in 1..2) {
            logs.add(
                SystemLog(
                    level = "INFO",
                    category = "SECURITY",
                    message = "SECURITY 로그 $i",
                    timestamp = LocalDateTime.now()
                )
            )
        }
        
        systemLogRepository.saveAll(logs)
        
        // when
        val appLogs = systemLogRepository.findByCategory("APP", PageRequest.of(0, 10))
        val securityLogs = systemLogRepository.findByCategory("SECURITY", PageRequest.of(0, 10))
        
        // then
        assertEquals(3, appLogs.content.size)
        assertEquals(2, securityLogs.content.size)
    }
    
    @Test
    @DisplayName("시간 범위별 로그 조회 테스트")
    fun testFindByTimestampBetween() {
        // given
        val now = LocalDateTime.now()
        val logs = mutableListOf<SystemLog>()
        
        // 1일 전 로그
        for (i in 1..3) {
            logs.add(
                SystemLog(
                    level = "INFO",
                    category = "TEST",
                    message = "1일 전 로그 $i",
                    timestamp = now.minusDays(1)
                )
            )
        }
        
        // 현재 로그
        for (i in 1..2) {
            logs.add(
                SystemLog(
                    level = "INFO",
                    category = "TEST",
                    message = "현재 로그 $i",
                    timestamp = now
                )
            )
        }
        
        systemLogRepository.saveAll(logs)
        
        // when
        val startTime = now.minusHours(1)
        val endTime = now.plusHours(1)
        val recentLogs = systemLogRepository.findByTimestampBetween(startTime, endTime, PageRequest.of(0, 10))
        
        // then
        assertEquals(2, recentLogs.content.size)
    }
    
    @Test
    @DisplayName("키워드 검색 테스트")
    fun testSearchByKeyword() {
        // given
        val logs = mutableListOf<SystemLog>()
        logs.add(
            SystemLog(
                level = "INFO",
                category = "TEST",
                message = "테스트 로그 성공",
                timestamp = LocalDateTime.now()
            )
        )
        
        logs.add(
            SystemLog(
                level = "ERROR",
                category = "TEST",
                message = "테스트 로그 실패",
                timestamp = LocalDateTime.now()
            )
        )
        
        logs.add(
            SystemLog(
                level = "INFO",
                category = "TEST",
                message = "다른 메시지",
                timestamp = LocalDateTime.now()
            )
        )
        
        systemLogRepository.saveAll(logs)
        
        // when
        val successLogs = systemLogRepository.searchByKeyword("성공", PageRequest.of(0, 10))
        val testLogs = systemLogRepository.searchByKeyword("테스트", PageRequest.of(0, 10))
        
        // then
        assertEquals(1, successLogs.content.size)
        assertEquals(2, testLogs.content.size)
    }
} 