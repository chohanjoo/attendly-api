package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.GbsMemberHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GbsMemberHistoryRepository : JpaRepository<GbsMemberHistory, Long>, GbsMemberHistoryRepositoryCustom {
} 
 