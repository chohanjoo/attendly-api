package com.attendly.domain.repository

import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.entity.VillageLeaderId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface VillageLeaderRepository : JpaRepository<VillageLeader, VillageLeaderId> {
    fun findByVillageIdAndEndDateIsNull(villageId: Long): VillageLeader?
    fun findByUserIdAndEndDateIsNull(userId: Long): VillageLeader?
    fun existsByVillageIdAndEndDateIsNull(villageId: Long): Boolean
    fun existsByUserIdAndEndDateIsNull(userId: Long): Boolean
    fun findByVillageIdAndUserIdAndEndDateIsNull(villageId: Long, userId: Long): VillageLeader?
} 