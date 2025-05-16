package com.attendly.service

import com.attendly.api.dto.VillageStatistics
import com.attendly.api.dto.minister.DepartmentStatisticsResponse
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.repository.AttendanceRepository
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.domain.repository.VillageRepository
import com.attendly.exception.AttendlyApiException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.ByteArrayOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.opencsv.CSVReader
import java.io.InputStreamReader
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import io.mockk.spyk

@ExtendWith(MockKExtension::class)
class MinisterStatisticsServiceTest {

    @MockK
    private lateinit var attendanceRepository: AttendanceRepository

    @MockK
    private lateinit var departmentRepository: DepartmentRepository

    @MockK
    private lateinit var villageRepository: VillageRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var organizationService: OrganizationService

    @MockK
    private lateinit var statisticsService: StatisticsService

    @InjectMockKs
    private lateinit var ministerStatisticsService: MinisterStatisticsService

    private lateinit var department: Department
    private lateinit var village1: Village
    private lateinit var village2: Village
    private val startDate = LocalDate.of(2023, 9, 1)
    private val endDate = LocalDate.of(2023, 9, 30)

    @BeforeEach
    fun setUp() {
        department = Department(
            id = 1L,
            name = "대학부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        village1 = Village(
            id = 1L,
            name = "동문 마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        village2 = Village(
            id = 2L,
            name = "서문 마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    @Test
    @DisplayName("부서 통계 요약 조회 성공 테스트")
    fun getDepartmentStatisticsSuccess() {
        // Given
        val villageStatistics1 = VillageStatistics(
            villageId = 1L,
            villageName = "동문 마을",
            gbsStats = emptyList(),
            totalMembers = 20,
            attendedMembers = 18,
            attendanceRate = 90.0,
            averageQtCount = 4.5
        )

        val villageStatistics2 = VillageStatistics(
            villageId = 2L,
            villageName = "서문 마을",
            gbsStats = emptyList(),
            totalMembers = 25,
            attendedMembers = 20,
            attendanceRate = 80.0,
            averageQtCount = 4.0
        )

        // 부서 조회 모킹
        every { departmentRepository.findById(1L) } returns java.util.Optional.of(department)
        
        // 마을 조회 모킹
        every { villageRepository.findByDepartment(department) } returns listOf(village1, village2)
        
        // 부서 멤버 수 모킹
        every { userRepository.countByDepartment(department) } returns 45
        
        // 마을별 통계 모킹
        every { statisticsService.getVillageStatistics(1L, startDate, endDate) } returns villageStatistics1
        every { statisticsService.getVillageStatistics(2L, startDate, endDate) } returns villageStatistics2
        
        // 주간 출석 데이터 모킹
        every { attendanceRepository.findByVillageIdAndWeek(any(), any()) } returns emptyList()

        // When
        val result = ministerStatisticsService.getDepartmentStatistics(1L, startDate, endDate)

        // Then
        assertNotNull(result)
        assertEquals(1L, result.departmentId)
        assertEquals("대학부", result.departmentName)
        assertEquals(45, result.totalMembers)
        assertEquals(38, result.attendedMembers)
        
        // 출석률 검증: (18 + 20) / 45 * 100 = 84.44%
        assertTrue(result.attendanceRate in 84.0..85.0)
        
        // 평균 QT 횟수 검증: ((4.5 * 20) + (4.0 * 25)) / 45 = 4.22
        assertTrue(result.averageQtCount in 4.2..4.3)
        
        // 마을 통계 검증
        assertEquals(2, result.villages.size)
        assertEquals(1L, result.villages[0].villageId)
        assertEquals(2L, result.villages[1].villageId)
        
        // 주간 통계 검증 (최소 1개 이상의 주간 데이터가 있어야 함)
        assertTrue(result.weeklyStats.isNotEmpty())
    }

    @Test
    @DisplayName("존재하지 않는 부서 ID로 조회 시 예외 발생")
    fun getDepartmentStatistics_nonExistingDepartmentId_throwsException() {
        // Given
        every { departmentRepository.findById(999L) } returns java.util.Optional.empty()

        // When & Then
        assertThrows<AttendlyApiException> {
            ministerStatisticsService.getDepartmentStatistics(999L, startDate, endDate)
        }
    }

    @Test
    @DisplayName("부서에 소속된 마을이 없을 경우 예외 발생")
    fun getDepartmentStatistics_noVillages_throwsException() {
        // Given
        every { departmentRepository.findById(1L) } returns java.util.Optional.of(department)
        every { villageRepository.findByDepartment(department) } returns emptyList()

        // When & Then
        assertThrows<AttendlyApiException> {
            ministerStatisticsService.getDepartmentStatistics(1L, startDate, endDate)
        }
    }
    
    @Test
    @DisplayName("부서 이름 조회 테스트")
    fun getDepartmentNameTest() {
        // Given
        every { departmentRepository.findById(1L) } returns java.util.Optional.of(department)
        
        // When
        val result = ministerStatisticsService.getDepartmentName(1L)
        
        // Then
        assertEquals("대학부", result)
    }
    
    @Test
    @DisplayName("존재하지 않는 부서 ID로 이름 조회 시 예외 발생")
    fun getDepartmentName_nonExistingDepartmentId_throwsException() {
        // Given
        every { departmentRepository.findById(999L) } returns java.util.Optional.empty()
        
        // When & Then
        assertThrows<AttendlyApiException> {
            ministerStatisticsService.getDepartmentName(999L)
        }
    }
    
    @Test
    @DisplayName("Excel 파일 내보내기 테스트")
    fun exportDepartmentStatisticsToExcelTest() {
        // Given
        val mockDepartmentStatistics = mockk<DepartmentStatisticsResponse>(relaxed = true)
        
        // 모의 통계 데이터 설정
        every { mockDepartmentStatistics.departmentId } returns 1L
        every { mockDepartmentStatistics.departmentName } returns "대학부"
        every { mockDepartmentStatistics.totalMembers } returns 45
        every { mockDepartmentStatistics.attendedMembers } returns 38
        every { mockDepartmentStatistics.attendanceRate } returns 84.44
        every { mockDepartmentStatistics.averageQtCount } returns 4.22
        every { mockDepartmentStatistics.villages } returns emptyList()
        every { mockDepartmentStatistics.weeklyStats } returns emptyList()
        
        // spyk를 사용하여 getDepartmentStatistics 메서드를 오버라이드
        val spyService = spyk(ministerStatisticsService)
        every { spyService.getDepartmentStatistics(1L, startDate, endDate) } returns mockDepartmentStatistics
        
        // 출력 스트림
        val outputStream = ByteArrayOutputStream()
        
        // When
        spyService.exportDepartmentStatisticsToExcel(1L, startDate, endDate, outputStream)
        
        // Then
        val bytes = outputStream.toByteArray()
        assertTrue(bytes.isNotEmpty())
        
        // Excel 워크북으로 파싱하여 검증 (기본적인 형식 검증)
        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        
        // 워크시트 수 확인
        assertEquals(3, workbook.numberOfSheets)
        
        // 시트 이름 확인
        assertEquals("요약", workbook.getSheetName(0))
        assertEquals("마을별 통계", workbook.getSheetName(1))
        assertEquals("주간 통계", workbook.getSheetName(2))
        
        // 요약 시트 내용 확인
        val summarySheet = workbook.getSheet("요약")
        assertTrue(summarySheet.getRow(1).getCell(1).stringCellValue == "대학부")
        
        workbook.close()
    }
    
    @Test
    @DisplayName("CSV 파일 내보내기 테스트")
    fun exportDepartmentStatisticsToCSVTest() {
        // Given
        val mockDepartmentStatistics = mockk<DepartmentStatisticsResponse>(relaxed = true)
        
        // 모의 통계 데이터 설정
        every { mockDepartmentStatistics.departmentId } returns 1L
        every { mockDepartmentStatistics.departmentName } returns "대학부"
        every { mockDepartmentStatistics.totalMembers } returns 45
        every { mockDepartmentStatistics.attendedMembers } returns 38
        every { mockDepartmentStatistics.attendanceRate } returns 84.44
        every { mockDepartmentStatistics.averageQtCount } returns 4.22
        every { mockDepartmentStatistics.villages } returns emptyList()
        every { mockDepartmentStatistics.weeklyStats } returns emptyList()
        
        // spyk를 사용하여 getDepartmentStatistics 메서드를 오버라이드
        val spyService = spyk(ministerStatisticsService)
        every { spyService.getDepartmentStatistics(1L, startDate, endDate) } returns mockDepartmentStatistics
        
        // 출력 스트림
        val outputStream = ByteArrayOutputStream()
        
        // When
        spyService.exportDepartmentStatisticsToCSV(1L, startDate, endDate, outputStream)
        
        // Then
        val bytes = outputStream.toByteArray()
        assertTrue(bytes.isNotEmpty())
        
        // CSV 파일로 파싱하여 검증
        val csvReader = CSVReader(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))
        val rows = csvReader.readAll()
        
        // 기본 행 수 확인 (최소한 헤더와 데이터가 있어야 함)
        assertTrue(rows.size > 5)
        
        // 부서명 확인
        assertEquals("부서명", rows[1][0])
        assertEquals("대학부", rows[1][1])
        
        csvReader.close()
    }
} 