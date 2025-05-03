package com.church.attendly.api.controller

import com.church.attendly.api.dto.LeaderDelegationResponse
import com.church.attendly.service.DelegationCreateRequest
import com.church.attendly.service.LeaderDelegationService
import com.church.attendly.domain.entity.LeaderDelegation
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/delegations")
class LeaderDelegationController(
    private val leaderDelegationService: LeaderDelegationService
) {

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    fun createDelegation(@RequestBody request: DelegationCreateRequest): ResponseEntity<LeaderDelegationResponse> {
        val delegation = leaderDelegationService.createDelegation(request)
        return ResponseEntity.ok(LeaderDelegationResponse.from(delegation))
    }

    @GetMapping("/active")
    fun getActiveDelegations(
        @RequestParam userId: Long,
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<List<LeaderDelegationResponse>> {
        val delegations = leaderDelegationService.findActiveDelegations(userId, date ?: LocalDate.now())
        return ResponseEntity.ok(delegations.map { LeaderDelegationResponse.from(it) })
    }
} 