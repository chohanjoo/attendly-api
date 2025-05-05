package com.attendly.domain.repository

import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GbsGroupRepository : JpaRepository<GbsGroup, Long>, GbsGroupRepositoryCustom {
    fun findByVillage(village: Village): List<GbsGroup>
} 