package com.attendly.api.controller

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.GbsMembersListResponse
import com.attendly.api.dto.LeaderGbsHistoryListResponse
import com.attendly.api.dto.LeaderGbsResponse
import com.attendly.api.util.ResponseUtil
import com.attendly.service.GbsMemberService
import com.attendly.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/gbs-members")
@Tag(name = "GBS 멤버 관리", description = "GBS 멤버 조회 API")
class GbsMemberController(
    private val gbsMemberService: GbsMemberService,
    private val userService: UserService
) {

    @GetMapping("/{gbsId}")
    @Operation(summary = "GBS 멤버 조회", description = "특정 GBS에 속한 멤버들의 정보를 조회합니다. 리더나 위임받은 리더만 자신의 GBS를 조회할 수 있습니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MINISTER', 'VILLAGE_LEADER', 'LEADER')")
    fun getGbsMembers(
        @PathVariable gbsId: Long,
        @RequestParam(required = false) date: LocalDate?,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<GbsMembersListResponse>> {
        val currentUser = userService.getCurrentUser(authentication)
        val targetDate = date ?: LocalDate.now()
        
        val response = gbsMemberService.getGbsMembers(gbsId, targetDate, currentUser)
        return ResponseUtil.success(response)
    }
    
    @GetMapping("/my-gbs")
    @Operation(summary = "로그인한 리더의 GBS 조회", description = "현재 로그인한 리더가 담당하는 GBS 정보를 조회합니다.")
    @PreAuthorize("hasRole('LEADER')")
    fun getMyGbs(authentication: Authentication): ResponseEntity<ApiResponse<LeaderGbsResponse>> {
        val currentUser = userService.getCurrentUser(authentication)
        val response = gbsMemberService.getGbsForLeader(currentUser.id!!)
        return ResponseUtil.success(response)
    }
    
    @GetMapping("/leaders/{leaderId}/history")
    @Operation(
        summary = "리더의 GBS 히스토리 조회", 
        description = "특정 리더가 지금까지 참여했던 GBS 히스토리 정보를 조회합니다. 각 GBS 히스토리에는 참여 기간과 조원 정보가 포함됩니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MINISTER', 'VILLAGE_LEADER', 'LEADER')")
    fun getLeaderGbsHistories(
        @PathVariable leaderId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<LeaderGbsHistoryListResponse>> {
        val currentUser = userService.getCurrentUser(authentication)
        val response = gbsMemberService.getLeaderGbsHistories(leaderId, currentUser)
        return ResponseUtil.success(response)
    }
} 