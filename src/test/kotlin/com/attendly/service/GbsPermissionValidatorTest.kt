package com.attendly.service

import com.attendly.api.dto.LeaderGbsHistoryRequestDto
import com.attendly.domain.entity.User
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.entity.Department
import com.attendly.domain.repository.GbsLeaderHistoryRepository
import com.attendly.domain.repository.LeaderDelegationRepository
import com.attendly.enums.Role
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.LocalDate

@DisplayName("GbsPermissionValidator 테스트")
class GbsPermissionValidatorTest {
    
    private val gbsLeaderHistoryRepository = mockk<GbsLeaderHistoryRepository>()
    private val leaderDelegationRepository = mockk<LeaderDelegationRepository>()
    private val organizationService = mockk<OrganizationService>()
    
    private val gbsPermissionValidator = GbsPermissionValidator(
        gbsLeaderHistoryRepository,
        leaderDelegationRepository,
        organizationService
    )
    
    private val mockDepartment = mockk<Department>()
    
    @Nested
    @DisplayName("validateGbsAccessPermission 테스트")
    inner class ValidateGbsAccessPermissionTest {
        
        @Test
        @DisplayName("관리자는 모든 GBS에 접근 가능")
        fun `관리자는 모든 GBS에 접근 가능`() {
            // Given
            val adminUser = User(id = 1L, name = "관리자", email = "admin@test.com", role = Role.ADMIN, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, adminUser)
        }
        
        @Test
        @DisplayName("교역자는 모든 GBS에 접근 가능")
        fun `교역자는 모든 GBS에 접근 가능`() {
            // Given
            val ministerUser = User(id = 2L, name = "교역자", email = "minister@test.com", role = Role.MINISTER, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, ministerUser)
        }
        
        @Test
        @DisplayName("마을장은 자신의 마을 GBS에 접근 가능")
        fun `마을장은 자신의 마을 GBS에 접근 가능`() {
            // Given
            val village = Village(id = 1L, name = "마을1", department = mockDepartment)
            val villageLeader = VillageLeader(village = village, user = mockk(), startDate = LocalDate.now())
            val villageLeaderUser = User(id = 3L, name = "마을장", email = "village@test.com", role = Role.VILLAGE_LEADER, department = mockDepartment, villageLeader = villageLeader)
            val gbsGroup = GbsGroup(id = 1L, name = "GBS1", village = village, termStartDate = LocalDate.now(), termEndDate = LocalDate.now().plusMonths(6))
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            every { organizationService.getGbsGroupById(gbsId) } returns gbsGroup
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, villageLeaderUser)
        }
        
        @Test
        @DisplayName("리더는 자신이 담당하는 GBS에 접근 가능")
        fun `리더는 자신이 담당하는 GBS에 접근 가능`() {
            // Given
            val leaderUser = User(id = 4L, name = "리더", email = "leader@test.com", role = Role.LEADER, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, leaderUser.id!!) } returns mockk<GbsLeaderHistory>()
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, leaderUser)
        }
        
        @Test
        @DisplayName("리더는 위임받은 GBS에 접근 가능")
        fun `리더는 위임받은 GBS에 접근 가능`() {
            // Given
            val delegateeUser = User(id = 5L, name = "위임받은리더", email = "delegatee@test.com", role = Role.LEADER, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, delegateeUser.id!!) } returns null
            every { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, delegateeUser.id!!, targetDate) } returns mockk<LeaderDelegation>()
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, delegateeUser)
        }
        
        @Test
        @DisplayName("권한이 없는 리더는 접근 불가")
        fun `권한이 없는 리더는 접근 불가`() {
            // Given
            val leaderUser = User(id = 6L, name = "권한없는리더", email = "noauth@test.com", role = Role.LEADER, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            every { gbsLeaderHistoryRepository.findCurrentLeaderHistoryByGbsIdAndLeaderId(gbsId, leaderUser.id!!) } returns null
            every { leaderDelegationRepository.findActiveByGbsIdAndDelegateeId(gbsId, leaderUser.id!!, targetDate) } returns null
            
            // When & Then
            assertThrows<AttendlyApiException> {
                gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, leaderUser)
            }
        }
        
        @Test
        @DisplayName("일반 사용자는 접근 불가")
        fun `일반 사용자는 접근 불가`() {
            // Given
            val normalUser = User(id = 7L, name = "일반사용자", email = "normal@test.com", role = Role.MEMBER, department = mockDepartment)
            val gbsId = 1L
            val targetDate = LocalDate.now()
            
            // When & Then
            assertThrows<AttendlyApiException> {
                gbsPermissionValidator.validateGbsAccessPermission(gbsId, targetDate, normalUser)
            }
        }
    }
    
    @Nested
    @DisplayName("validateLeaderHistoryAccessPermission 테스트")
    inner class ValidateLeaderHistoryAccessPermissionTest {
        
        @Test
        @DisplayName("자신의 히스토리는 조회 가능")
        fun `자신의 히스토리는 조회 가능`() {
            // Given
            val currentUser = User(id = 1L, name = "사용자", email = "user@test.com", role = Role.LEADER, department = mockDepartment)
            val request = LeaderGbsHistoryRequestDto(currentUser.id!!, currentUser)
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
        }
        
        @Test
        @DisplayName("관리자는 모든 히스토리 조회 가능")
        fun `관리자는 모든 히스토리 조회 가능`() {
            // Given
            val adminUser = User(id = 1L, name = "관리자", email = "admin@test.com", role = Role.ADMIN, department = mockDepartment)
            val request = LeaderGbsHistoryRequestDto(2L, adminUser)
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
        }
        
        @Test
        @DisplayName("교역자는 모든 히스토리 조회 가능")
        fun `교역자는 모든 히스토리 조회 가능`() {
            // Given
            val ministerUser = User(id = 1L, name = "교역자", email = "minister@test.com", role = Role.MINISTER, department = mockDepartment)
            val request = LeaderGbsHistoryRequestDto(2L, ministerUser)
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
        }
        
        @Test
        @DisplayName("마을장은 자신 마을의 리더 히스토리 조회 가능")
        fun `마을장은 자신 마을의 리더 히스토리 조회 가능`() {
            // Given
            val village = Village(id = 1L, name = "마을1", department = mockDepartment)
            val villageLeader = VillageLeader(village = village, user = mockk(), startDate = LocalDate.now())
            val villageLeaderUser = User(id = 1L, name = "마을장", email = "village@test.com", role = Role.VILLAGE_LEADER, department = mockDepartment, villageLeader = villageLeader)
            val request = LeaderGbsHistoryRequestDto(2L, villageLeaderUser)
            
            // When & Then (예외가 발생하지 않아야 함)
            gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
        }
        
        @Test
        @DisplayName("권한이 없는 사용자는 타인 히스토리 조회 불가")
        fun `권한이 없는 사용자는 타인 히스토리 조회 불가`() {
            // Given
            val normalUser = User(id = 1L, name = "일반사용자", email = "normal@test.com", role = Role.MEMBER, department = mockDepartment)
            val request = LeaderGbsHistoryRequestDto(2L, normalUser)
            
            // When & Then
            assertThrows<AttendlyApiException> {
                gbsPermissionValidator.validateLeaderHistoryAccessPermission(request)
            }
        }
    }
} 