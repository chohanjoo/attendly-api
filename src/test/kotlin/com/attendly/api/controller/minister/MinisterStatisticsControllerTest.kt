package com.attendly.api.controller.minister

import com.attendly.api.dto.minister.*
import com.attendly.security.CustomUserDetailsService
import com.attendly.security.JwtTokenProvider
import com.attendly.security.TestSecurityConfig
import com.attendly.service.MinisterStatisticsService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.io.OutputStream
import java.time.LocalDate
import jakarta.servlet.http.HttpServletResponse

@WebMvcTest(MinisterStatisticsController::class)
@Import(TestSecurityConfig::class)
class MinisterStatisticsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var ministerStatisticsService: MinisterStatisticsService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var userDetailsService: CustomUserDetailsService

    private lateinit var departmentStatisticsResponse: DepartmentStatisticsResponse
    private lateinit var villageDetailStatisticsResponse: VillageDetailStatisticsResponse

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        // 부서 통계 응답 모의 데이터 설정
        departmentStatisticsResponse = DepartmentStatisticsResponse(
            departmentId = 1L,
            departmentName = "대학부",
            totalMembers = 120,
            attendedMembers = 98,
            attendanceRate = 81.7,
            averageQtCount = 4.2,
            villages = listOf(
                VillageStatisticsItem(
                    villageId = 1L,
                    villageName = "동문 마을",
                    totalMembers = 24,
                    attendedMembers = 22,
                    attendanceRate = 91.7,
                    averageQtCount = 4.8
                )
            ),
            weeklyStats = listOf(
                WeeklyStatisticsItem(
                    weekStart = LocalDate.of(2023, 9, 3),
                    totalMembers = 120,
                    attendedMembers = 95,
                    attendanceRate = 79.2
                )
            )
        )

        // 마을 상세 통계 응답 모의 데이터 설정
        villageDetailStatisticsResponse = VillageDetailStatisticsResponse(
            villageId = 1L,
            villageName = "동문 마을",
            totalMembers = 24,
            attendedMembers = 22,
            attendanceRate = 91.7,
            averageQtCount = 4.8,
            members = listOf(
                MemberStatisticsItem(
                    userId = 1L,
                    userName = "홍길동",
                    attendanceCount = 4,
                    attendanceRate = 100.0,
                    qtCount = 5
                )
            ),
            weeklyStats = listOf(
                VillageWeeklyStatisticsItem(
                    weekStart = "2023-09-03",
                    totalMembers = 24,
                    attendedMembers = 22,
                    attendanceRate = 91.7
                )
            )
        )
    }

    @Test
    @DisplayName("교역자 권한으로 부서 통계 요약 조회 성공")
    @WithMockUser(username = "minister@attendly.com", roles = ["MINISTER"])
    fun testGetDepartmentStatisticsWithMinisterRole() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)

        every {
            ministerStatisticsService.getDepartmentStatistics(departmentId, startDate, endDate)
        } returns departmentStatisticsResponse

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.departmentId").value(1))
            .andExpect(jsonPath("$.data.departmentName").value("대학부"))
            .andExpect(jsonPath("$.data.totalMembers").value(120))
            .andExpect(jsonPath("$.data.attendedMembers").value(98))
            .andExpect(jsonPath("$.data.attendanceRate").value(81.7))
            .andExpect(jsonPath("$.data.averageQtCount").value(4.2))
            .andExpect(jsonPath("$.data.villages").isArray)
            .andExpect(jsonPath("$.data.villages[0].villageId").value(1))
            .andExpect(jsonPath("$.data.weeklyStats").isArray)
            .andExpect(jsonPath("$.data.weeklyStats[0].totalMembers").value(120))

        verify { ministerStatisticsService.getDepartmentStatistics(departmentId, startDate, endDate) }
    }

    @Test
    @DisplayName("일반 사용자 권한으로 부서 통계 요약 조회 실패")
    @WithMockUser(username = "user@attendly.com", roles = ["USER"])
    fun testGetDepartmentStatisticsWithUserRole() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is5xxServerError)
    }
    
    @Test
    @DisplayName("교역자 권한으로 부서 통계 데이터 Excel 다운로드 성공")
    @WithMockUser(username = "minister@attendly.com", roles = ["MINISTER"])
    fun testDownloadDepartmentStatisticsAsExcelWithMinisterRole() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)
        val format = "xls"
        
        every { 
            ministerStatisticsService.getDepartmentName(departmentId) 
        } returns "대학부"
        
        every { 
            ministerStatisticsService.exportDepartmentStatisticsToExcel(
                eq(departmentId), 
                eq(startDate), 
                eq(endDate), 
                any() 
            ) 
        } answers { 
            // 네 번째 인자인 outputStream에 더미 데이터 작성
            val outputStream = arg<OutputStream>(3)
            outputStream.write("Excel file content".toByteArray())
        }

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics/download", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("format", format)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.ms-excel"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"대학부_통계_${startDate}_${endDate}.xls\""))
            
        verify { 
            ministerStatisticsService.getDepartmentName(departmentId)
            ministerStatisticsService.exportDepartmentStatisticsToExcel(
                eq(departmentId), 
                eq(startDate), 
                eq(endDate), 
                any()
            ) 
        }
    }
    
    @Test
    @DisplayName("교역자 권한으로 부서 통계 데이터 CSV 다운로드 성공")
    @WithMockUser(username = "minister@attendly.com", roles = ["MINISTER"])
    fun testDownloadDepartmentStatisticsAsCSVWithMinisterRole() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)
        val format = "csv"
        
        every { 
            ministerStatisticsService.getDepartmentName(departmentId) 
        } returns "대학부"
        
        every { 
            ministerStatisticsService.exportDepartmentStatisticsToCSV(
                eq(departmentId), 
                eq(startDate), 
                eq(endDate), 
                any() 
            ) 
        } answers { 
            // 네 번째 인자인 outputStream에 더미 데이터 작성
            val outputStream = arg<OutputStream>(3)
            outputStream.write("CSV file content".toByteArray())
        }

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics/download", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("format", format)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("text/csv; charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"대학부_통계_${startDate}_${endDate}.csv\""))
            
        verify { 
            ministerStatisticsService.getDepartmentName(departmentId)
            ministerStatisticsService.exportDepartmentStatisticsToCSV(
                eq(departmentId), 
                eq(startDate), 
                eq(endDate), 
                any()
            ) 
        }
    }
    
    @Test
    @DisplayName("지원하지 않는 형식으로 부서 통계 데이터 다운로드 시 실패")
    @WithMockUser(username = "minister@attendly.com", roles = ["MINISTER"])
    fun testDownloadDepartmentStatisticsWithUnsupportedFormat() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)
        val format = "pdf" // 지원하지 않는 형식
        
        every { 
            ministerStatisticsService.getDepartmentName(departmentId) 
        } returns "대학부"

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics/download", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("format", format)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            
        verify { ministerStatisticsService.getDepartmentName(departmentId) }
    }
    
    @Test
    @DisplayName("일반 사용자 권한으로 부서 통계 데이터 다운로드 실패")
    @WithMockUser(username = "user@attendly.com", roles = ["USER"])
    fun testDownloadDepartmentStatisticsWithUserRole() {
        // given
        val departmentId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)
        val format = "xls"

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/statistics/download", departmentId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .param("format", format)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @DisplayName("교역자 권한으로 마을별 상세 통계 조회 성공")
    @WithMockUser(username = "minister@attendly.com", roles = ["MINISTER"])
    fun testGetVillageDetailStatisticsWithMinisterRole() {
        // given
        val departmentId = 1L
        val villageId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)

        every {
            ministerStatisticsService.getVillageDetailStatistics(departmentId, villageId, startDate, endDate)
        } returns villageDetailStatisticsResponse

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/villages/{villageId}/statistics", departmentId, villageId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.villageId").value(1))
            .andExpect(jsonPath("$.data.villageName").value("동문 마을"))
            .andExpect(jsonPath("$.data.totalMembers").value(24))
            .andExpect(jsonPath("$.data.attendedMembers").value(22))
            .andExpect(jsonPath("$.data.attendanceRate").value(91.7))
            .andExpect(jsonPath("$.data.averageQtCount").value(4.8))
            .andExpect(jsonPath("$.data.members").isArray)
            .andExpect(jsonPath("$.data.members[0].userId").value(1))
            .andExpect(jsonPath("$.data.members[0].userName").value("홍길동"))
            .andExpect(jsonPath("$.data.members[0].attendanceCount").value(4))
            .andExpect(jsonPath("$.data.members[0].attendanceRate").value(100.0))
            .andExpect(jsonPath("$.data.members[0].qtCount").value(5))
            .andExpect(jsonPath("$.data.weeklyStats").isArray)
            .andExpect(jsonPath("$.data.weeklyStats[0].weekStart").value("2023-09-03"))
            .andExpect(jsonPath("$.data.weeklyStats[0].totalMembers").value(24))
            .andExpect(jsonPath("$.data.weeklyStats[0].attendedMembers").value(22))
            .andExpect(jsonPath("$.data.weeklyStats[0].attendanceRate").value(91.7))

        verify { ministerStatisticsService.getVillageDetailStatistics(departmentId, villageId, startDate, endDate) }
    }

    @Test
    @DisplayName("일반 사용자 권한으로 마을별 상세 통계 조회 실패")
    @WithMockUser(username = "user@attendly.com", roles = ["USER"])
    fun testGetVillageDetailStatisticsWithUserRole() {
        // given
        val departmentId = 1L
        val villageId = 1L
        val startDate = LocalDate.of(2023, 9, 1)
        val endDate = LocalDate.of(2023, 9, 30)

        // when & then
        mockMvc.perform(
            get("/api/minister/departments/{departmentId}/villages/{villageId}/statistics", departmentId, villageId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is5xxServerError)
    }
} 