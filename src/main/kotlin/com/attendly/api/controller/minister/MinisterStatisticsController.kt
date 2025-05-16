package com.attendly.api.controller.minister

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.minister.DepartmentStatisticsResponse
import com.attendly.api.dto.minister.VillageDetailStatisticsResponse
import com.attendly.api.util.ResponseUtil
import com.attendly.service.MinisterStatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/minister")
@Tag(name = "교역자 API", description = "교역자 전용 API")
class MinisterStatisticsController(
    private val ministerStatisticsService: MinisterStatisticsService
) {

    @GetMapping("/departments/{departmentId}/statistics")
    @Operation(
        summary = "부서 통계 요약 조회",
        description = "특정 부서의 통계 요약 정보를 조회합니다. 지정된 기간 동안의 출석 현황, 마을별 통계, 주간 통계 등을 확인할 수 있습니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "부서 통계 조회 성공",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = DepartmentStatisticsResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부서 또는 마을을 찾을 수 없음"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
        ]
    )
    @PreAuthorize("hasRole('MINISTER')")
    fun getDepartmentStatistics(
        @Parameter(description = "부서 ID", required = true)
        @PathVariable departmentId: Long,
        
        @Parameter(description = "시작 날짜 (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        
        @Parameter(description = "종료 날짜 (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<ApiResponse<DepartmentStatisticsResponse>> {
        val statistics = ministerStatisticsService.getDepartmentStatistics(departmentId, startDate, endDate)
        return ResponseUtil.success(
            data = statistics,
            message = "부서 통계 조회 성공"
        )
    }
    
    @GetMapping("/departments/{departmentId}/villages/{villageId}/statistics")
    @Operation(
        summary = "마을별 상세 통계 조회",
        description = "특정 마을의 상세 통계 정보를 조회합니다. 지정된 기간 동안의 출석 현황, 개인별 통계, 주간 통계 등을 확인할 수 있습니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "마을별 통계 조회 성공",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = VillageDetailStatisticsResponse::class))]
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "부서 또는 마을을 찾을 수 없음"),
            io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
        ]
    )
    @PreAuthorize("hasRole('MINISTER')")
    fun getVillageDetailStatistics(
        @Parameter(description = "부서 ID", required = true)
        @PathVariable departmentId: Long,
        
        @Parameter(description = "마을 ID", required = true)
        @PathVariable villageId: Long,
        
        @Parameter(description = "시작 날짜 (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        
        @Parameter(description = "종료 날짜 (yyyy-MM-dd)", required = true)
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<ApiResponse<VillageDetailStatisticsResponse>> {
        val statistics = ministerStatisticsService.getVillageDetailStatistics(departmentId, villageId, startDate, endDate)
        return ResponseUtil.success(
            data = statistics,
            message = "마을별 통계 조회 성공"
        )
    }
} 