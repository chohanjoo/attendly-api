package com.attendly.domain.repository

import com.attendly.domain.entity.VillageLeader
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VillageLeaderRepository : JpaRepository<VillageLeader, Long> {
    fun findByVillageIdAndEndDateIsNull(villageId: Long): VillageLeader?
} 