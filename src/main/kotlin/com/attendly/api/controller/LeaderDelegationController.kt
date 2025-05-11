package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.LeaderDelegationResponse
import com.attendly.api.dto.PageResponse
import com.attendly.api.util.ResponseUtil
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
    fun createDelegation(@RequestBody request: DelegationCreateRequest): ResponseEntity<ApiResponse<LeaderDelegationResponse>> {
        val delegation = leaderDelegationService.createDelegation(request)
        return ResponseUtil.created(LeaderDelegationResponse.from(delegation))
    }

    @GetMapping("/active")
    fun getActiveDelegations(
        @RequestParam userId: Long,
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<ApiResponse<PageResponse<LeaderDelegationResponse>>> {
        val delegations = leaderDelegationService.findActiveDelegations(userId, date ?: LocalDate.now())
        val responseList = delegations.map { LeaderDelegationResponse.from(it) }
        return ResponseUtil.successList(responseList)
    }
} 