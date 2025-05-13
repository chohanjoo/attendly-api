package com.attendly.service

import com.attendly.api.dto.PageResponse
import com.attendly.api.dto.admin.AdminAttendanceResponse
import com.attendly.api.dto.admin.AdminAttendanceSearchRequest
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.enums.AttendanceStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminAttendanceService(
    private val attendanceRepository: AttendanceRepository
) {
    
    @Transactional(readOnly = true)
    fun getAttendancesForAdmin(request: AdminAttendanceSearchRequest): PageResponse<AdminAttendanceResponse> {
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "weekStart")
        )
        
        val attendancesPage = attendanceRepository.findAttendancesForAdmin(
            search = request.search,
            startDate = request.startDate,
            endDate = request.endDate,
            status = request.status,
            pageable = pageable
        )
        
        val attendanceResponses = attendancesPage.content.map { attendance ->
            AdminAttendanceResponse.fromEntity(
                id = attendance.id ?: -1,
                userId = attendance.member.id ?: -1,
                userName = attendance.member.name,
                date = attendance.weekStart,
                worship = attendance.worship,
                qtCount = attendance.qtCount,
                ministry = attendance.ministry
            )
        }
        
        return PageResponse(
            items = attendanceResponses,
            totalCount = attendancesPage.totalElements,
            hasMore = attendancesPage.number + 1 < attendancesPage.totalPages
        )
    }
} 