package com.church.attendly.domain.repository

import com.church.attendly.domain.entity.LeaderDelegation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface LeaderDelegationRepository : JpaRepository<LeaderDelegation, Long>, LeaderDelegationRepositoryCustom 