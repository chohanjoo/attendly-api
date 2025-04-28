package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface GbsLeaderHistoryRepository : JpaRepository<GbsLeaderHistory, Long>, GbsLeaderHistoryRepositoryCustom {
} 