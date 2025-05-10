package com.attendly.domain.model

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.User

/**
 * GBS 그룹과 현재 리더 정보를 함께 담는 데이터 클래스
 */
data class GbsWithLeader(
    val gbsGroup: GbsGroup,
    val leader: User?
) 