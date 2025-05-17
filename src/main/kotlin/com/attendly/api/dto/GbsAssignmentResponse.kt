package com.attendly.api.dto

import java.time.LocalDateTime

/**
 * GBS 배치 정보 조회 응답 DTO
 */
data class GbsAssignmentResponse(
    val villageId: Long,
    val villageName: String,
    val columns: List<KanbanColumn>,
    val labels: List<KanbanLabel>
)

/**
 * 칸반 보드 컬럼 정보
 */
data class KanbanColumn(
    val id: String,
    val title: String,
    val cards: List<KanbanCard>
)

/**
 * 칸반 보드 카드 정보
 */
data class KanbanCard(
    val id: String,
    val content: String,
    val labels: List<KanbanLabel>
)

/**
 * 칸반 보드 라벨 정보
 */
data class KanbanLabel(
    val id: Long,
    val name: String,
    val color: String
) 