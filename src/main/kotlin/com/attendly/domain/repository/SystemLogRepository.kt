package com.attendly.domain.repository

import com.attendly.domain.entity.SystemLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SystemLogRepository : JpaRepository<SystemLog, Long>, SystemLogRepositoryCustom {
    
    /**
     * 레벨, 카테고리, 기간으로 로그를 검색
     */
    fun findByLevelAndCategoryAndTimestampBetween(
        level: String,
        category: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        pageable: Pageable
    ): Page<SystemLog>
    
    /**
     * 레벨로 로그를 검색
     */
    fun findByLevel(level: String, pageable: Pageable): Page<SystemLog>
    
    /**
     * 카테고리로 로그를 검색
     */
    fun findByCategory(category: String, pageable: Pageable): Page<SystemLog>
    
    /**
     * 기간으로 로그를 검색
     */
    fun findByTimestampBetween(startTime: LocalDateTime, endTime: LocalDateTime, pageable: Pageable): Page<SystemLog>
    
    /**
     * 특정 사용자 관련 로그를 검색
     */
    fun findByUserId(userId: Long, pageable: Pageable): Page<SystemLog>
    
    /**
     * 메시지 내용으로 검색
     */
    @Query("SELECT s FROM SystemLog s WHERE LOWER(s.message) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    fun searchByKeyword(@Param("keyword") keyword: String, pageable: Pageable): Page<SystemLog>
}