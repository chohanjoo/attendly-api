package com.attendly.security

import com.attendly.enums.Role
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.repository.VillageRepository
import com.attendly.service.AttendanceService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class MethodSecurityExpressionsTest {

    private lateinit var methodSecurityExpressions: MethodSecurityExpressions
    private lateinit var attendanceService: AttendanceService
    private lateinit var villageRepository: VillageRepository
    private lateinit var authentication: Authentication
    private lateinit var securityContext: SecurityContext
    private lateinit var userDetailsAdapter: UserDetailsAdapter
    private lateinit var adminUser: User
    private lateinit var ministerUser: User
    private lateinit var villageLeaderUser: User
    private lateinit var leaderUser: User
    private lateinit var memberUser: User
    private lateinit var village: Village
    private lateinit var villageLeader: VillageLeader

    @BeforeEach
    fun setUp() {
        attendanceService = mockk(relaxed = true)
        villageRepository = mockk(relaxed = true)
        authentication = mockk(relaxed = true)
        securityContext = mockk(relaxed = true)
        
        methodSecurityExpressions = MethodSecurityExpressions(attendanceService, villageRepository)
        
        // 테스트 사용자 생성
        adminUser = mockk(relaxed = true)
        every { adminUser.role } returns Role.ADMIN
        every { adminUser.id } returns 1L
        
        ministerUser = mockk(relaxed = true)
        every { ministerUser.role } returns Role.MINISTER
        every { ministerUser.id } returns 2L
        
        villageLeaderUser = mockk(relaxed = true)
        every { villageLeaderUser.role } returns Role.VILLAGE_LEADER
        every { villageLeaderUser.id } returns 3L
        
        leaderUser = mockk(relaxed = true)
        every { leaderUser.role } returns Role.LEADER
        every { leaderUser.id } returns 4L
        
        memberUser = mockk(relaxed = true)
        every { memberUser.role } returns Role.MEMBER
        every { memberUser.id } returns 5L
        
        // 마을 및 마을장 설정
        village = mockk(relaxed = true)
        every { village.id } returns 1L
        
        villageLeader = mockk(relaxed = true)
        every { villageLeader.village } returns village
        
        every { villageLeaderUser.villageLeader } returns villageLeader
        
        // SecurityContext 설정
        SecurityContextHolder.setContext(securityContext)
        every { securityContext.authentication } returns authentication
    }

    @Test
    @DisplayName("canManageGbsAttendance - 관리자는 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_AdminHasAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns adminUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 교역자는 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_MinisterHasAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns ministerUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 마을장은 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_VillageLeaderHasAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns villageLeaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 리더는 자신의 GBS에 접근 가능")
    fun canManageGbsAttendance_LeaderHasAccessToOwnGbs() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns leaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        val gbsId = 1L
        every { leaderUser.id } returns 4L
        every { attendanceService.hasLeaderAccess(gbsId, 4L) } returns true
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(gbsId)
        
        // then
        assertTrue(result)
        verify(exactly = 1) { attendanceService.hasLeaderAccess(gbsId, 4L) }
    }

    @Test
    @DisplayName("canManageGbsAttendance - 리더는 자신의 GBS가 아닌 경우 접근 불가")
    fun canManageGbsAttendance_LeaderHasNoAccessToOtherGbs() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns leaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        val gbsId = 1L
        every { leaderUser.id } returns 4L
        every { attendanceService.hasLeaderAccess(gbsId, 4L) } returns false
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(gbsId)
        
        // then
        assertFalse(result)
        verify(exactly = 1) { attendanceService.hasLeaderAccess(gbsId, 4L) }
    }

    @Test
    @DisplayName("canManageGbsAttendance - 일반 회원은 GBS 접근 불가")
    fun canManageGbsAttendance_MemberHasNoAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns memberUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 인증되지 않은 사용자는 접근 불가")
    fun canManageGbsAttendance_UnauthenticatedHasNoAccess() {
        // given
        every { authentication.principal } returns null
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canAccessVillage - 관리자는 모든 마을에 접근 가능")
    fun canAccessVillage_AdminHasAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns adminUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canAccessVillage - 교역자는 모든 마을에 접근 가능")
    fun canAccessVillage_MinisterHasAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns ministerUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canAccessVillage - 마을장은 자신의 마을에만 접근 가능")
    fun canAccessVillage_VillageLeaderHasAccessToOwnVillage() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns villageLeaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        val villageId = 1L
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(villageId)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canAccessVillage - 마을장은 다른 마을에 접근 불가")
    fun canAccessVillage_VillageLeaderHasNoAccessToOtherVillage() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns villageLeaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        val otherVillageId = 2L
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(otherVillageId)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canAccessVillage - 리더는 마을 접근 불가")
    fun canAccessVillage_LeaderHasNoAccess() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns leaderUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canAccessVillage - 인증되지 않은 사용자는 접근 불가")
    fun canAccessVillage_UnauthenticatedHasNoAccess() {
        // given
        every { authentication.principal } returns null
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("isMinister - 교역자 권한 확인")
    fun isMinister_WithMinisterRole() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns ministerUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("isMinister - 교역자가 아닌 경우")
    fun isMinister_WithNonMinisterRole() {
        // given
        userDetailsAdapter = mockk(relaxed = true)
        every { userDetailsAdapter.getUser() } returns adminUser
        every { authentication.principal } returns userDetailsAdapter
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("isMinister - 인증되지 않은 사용자")
    fun isMinister_WithUnauthenticatedUser() {
        // given
        every { authentication.principal } returns null
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertFalse(result)
    }
} 