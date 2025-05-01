package com.church.attendly.api.controller

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

data class LeaderDelegationResponse(
    val id: Long,
    val delegatorId: Long,
    val delegatorName: String,
    val delegateeId: Long,
    val delegateeName: String,
    val gbsGroupId: Long,
    val gbsGroupName: String,
    val startDate: LocalDate,
    val endDate: LocalDate?
) {
    companion object {
        fun from(delegation: LeaderDelegation): LeaderDelegationResponse {
            return LeaderDelegationResponse(
                id = delegation.id!!,
                delegatorId = delegation.delegator.id!!,
                delegatorName = delegation.delegator.name,
                delegateeId = delegation.delegatee.id!!,
                delegateeName = delegation.delegatee.name,
                gbsGroupId = delegation.gbsGroup.id!!,
                gbsGroupName = delegation.gbsGroup.name,
                startDate = delegation.startDate,
                endDate = delegation.endDate
            )
        }
    }
} 