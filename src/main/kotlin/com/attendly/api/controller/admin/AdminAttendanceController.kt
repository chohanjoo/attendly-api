package com.attendly.api.controller.admin

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.admin.AdminAttendanceResponse
import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
import com.attendly.api.util.ResponseUtil
import com.attendly.enums.AttendanceStatus
import com.attendly.service.AdminAttendanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin")
@Tag(name = "관리자 출석 API", description = "관리자용 출석 데이터 관리 API")
class AdminAttendanceController(
    private val adminAttendanceService: AdminAttendanceService
) {

    @GetMapping("/attendance")
    @Operation(
        summary = "출석 데이터 관리자용 조회",
        description = "관리자 권한으로 출석 데이터를 다양한 조건으로 조회합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MINISTER')")
    fun getAttendances(
        @Parameter(description = "페이지 번호 (0부터 시작)")
        @RequestParam(defaultValue = "0") page: Int,
        
        @Parameter(description = "페이지 크기")
        @RequestParam(defaultValue = "20") size: Int,
        
        @Parameter(description = "검색어 (이름)")
        @RequestParam(required = false) search: String?,
        
        @Parameter(description = "시작 날짜 (yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        
        @Parameter(description = "종료 날짜 (yyyy-MM-dd)")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        
        @Parameter(description = "출석 상태 (PRESENT, ABSENT, LATE, EXCUSED)")
        @RequestParam(required = false) status: AttendanceStatus?
    ): ResponseEntity<ApiResponse<PageResponse<AdminAttendanceResponse>>> {
        val request = AdminAttendanceSearchRequest(
            page = page,
            size = size,
            search = search,
            startDate = startDate,
            endDate = endDate,
            status = status
        )
        
        val attendances = adminAttendanceService.getAttendancesForAdmin(request)
        return ResponseUtil.success(attendances)
    }
} 