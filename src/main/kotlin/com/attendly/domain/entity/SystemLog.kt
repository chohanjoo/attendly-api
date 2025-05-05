package com.attendly.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 시스템 로그를 저장하는 엔티티
 */
@Entity
@Table(name = "system_log")
class SystemLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * 로그 레벨 (INFO, WARN, ERROR 등)
     */
    @Column(nullable = false, length = 10)
    val level: String,

    /**
     * 로그 카테고리 (APPLICATION, SECURITY, BATCH, AUDIT 등)
     */
    @Column(nullable = false, length = 20)
    val category: String,

    /**
     * 로그 메시지
     */
    @Column(nullable = false, length = 1000)
    val message: String,

    /**
     * 추가 정보 (JSON 형태로 저장)
     */
    @Column(columnDefinition = "TEXT")
    val additionalInfo: String? = null,

    /**
     * 로그 발생 시간
     */
    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    /**
     * 요청 IP 주소
     */
    @Column(length = 39)
    val ipAddress: String? = null,

    /**
     * 사용자 ID (로그인된 경우)
     */
    @Column(name = "user_id")
    val userId: Long? = null,

    /**
     * User-Agent 정보
     */
    @Column(length = 255)
    val userAgent: String? = null,

    /**
     * 서버 인스턴스 이름
     */
    @Column(length = 50)
    val serverInstance: String? = null
) 