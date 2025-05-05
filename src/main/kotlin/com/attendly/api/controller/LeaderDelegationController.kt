package com.attendly.api.controller

import com.attendly.api.dto.LeaderDelegationResponse
import com.attendly.service.DelegationCreateRequest
import com.attendly.service.LeaderDelegationService
import com.attendly.domain.entity.LeaderDelegation
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