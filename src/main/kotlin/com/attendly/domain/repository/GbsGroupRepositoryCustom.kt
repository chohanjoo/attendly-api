package com.attendly.domain.repository

import com.attendly.api.dto.AdminGbsGroupQueryDto
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsWithLeader
import com.attendly.domain.model.VillageGbsWithLeaderInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface GbsGroupRepositoryCustom {
    fun findActiveGroupsByVillageId(villageId: Long, date: LocalDate): List<GbsGroup>
    fun findByVillageAndTermDate(village: Village, startDate: LocalDate, endDate: LocalDate): List<GbsGroup>
    fun findWithCurrentLeader(gbsId: Long): GbsWithLeader?
    fun findAllGbsGroupsWithDetails(pageable: Pageable): Page<GbsGroup>
    fun findAllGbsGroupsWithCompleteDetails(pageable: Pageable): Page<AdminGbsGroupQueryDto>
    
    /**
     * 마을의 모든 활성 GBS와 해당 리더 정보를 한 번에 조회합니다.
     * N+1 문제를 방지하기 위해 join 쿼리를 사용합니다.
     */
    fun findVillageGbsWithLeaderInfo(villageId: Long, date: LocalDate): List<VillageGbsWithLeaderInfo>
} 