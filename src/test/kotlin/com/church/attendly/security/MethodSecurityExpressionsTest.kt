package com.church.attendly.security

import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.entity.Village
import com.church.attendly.domain.entity.VillageLeader
import com.church.attendly.domain.repository.VillageRepository
import com.church.attendly.service.AttendanceService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
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
        attendanceService = mock(AttendanceService::class.java)
        villageRepository = mock(VillageRepository::class.java)
        authentication = mock(Authentication::class.java)
        securityContext = mock(SecurityContext::class.java)
        
        methodSecurityExpressions = MethodSecurityExpressions(attendanceService, villageRepository)
        
        // 테스트 사용자 생성
        adminUser = mock(User::class.java)
        `when`(adminUser.role).thenReturn(Role.ADMIN)
        `when`(adminUser.id).thenReturn(1L)
        
        ministerUser = mock(User::class.java)
        `when`(ministerUser.role).thenReturn(Role.MINISTER)
        `when`(ministerUser.id).thenReturn(2L)
        
        villageLeaderUser = mock(User::class.java)
        `when`(villageLeaderUser.role).thenReturn(Role.VILLAGE_LEADER)
        `when`(villageLeaderUser.id).thenReturn(3L)
        
        leaderUser = mock(User::class.java)
        `when`(leaderUser.role).thenReturn(Role.LEADER)
        `when`(leaderUser.id).thenReturn(4L)
        
        memberUser = mock(User::class.java)
        `when`(memberUser.role).thenReturn(Role.MEMBER)
        `when`(memberUser.id).thenReturn(5L)
        
        // 마을 및 마을장 설정
        village = mock(Village::class.java)
        `when`(village.id).thenReturn(1L)
        
        villageLeader = mock(VillageLeader::class.java)
        `when`(villageLeader.village).thenReturn(village)
        
        `when`(villageLeaderUser.villageLeader).thenReturn(villageLeader)
        
        // SecurityContext 설정
        SecurityContextHolder.setContext(securityContext)
        `when`(securityContext.authentication).thenReturn(authentication)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 관리자는 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_AdminHasAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(adminUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 교역자는 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_MinisterHasAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(ministerUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 마을장은 모든 GBS에 접근 가능")
    fun canManageGbsAttendance_VillageLeaderHasAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(villageLeaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 리더는 자신의 GBS에 접근 가능")
    fun canManageGbsAttendance_LeaderHasAccessToOwnGbs() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(leaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        val gbsId = 1L
        `when`(attendanceService.hasLeaderAccess(gbsId, leaderUser.id!!)).thenReturn(true)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(gbsId)
        
        // then
        assertTrue(result)
        verify(attendanceService).hasLeaderAccess(gbsId, leaderUser.id!!)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 리더는 자신의 GBS가 아닌 경우 접근 불가")
    fun canManageGbsAttendance_LeaderHasNoAccessToOtherGbs() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(leaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        val gbsId = 1L
        `when`(attendanceService.hasLeaderAccess(gbsId, leaderUser.id!!)).thenReturn(false)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(gbsId)
        
        // then
        assertFalse(result)
        verify(attendanceService).hasLeaderAccess(gbsId, leaderUser.id!!)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 일반 회원은 GBS 접근 불가")
    fun canManageGbsAttendance_MemberHasNoAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(memberUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canManageGbsAttendance - 인증되지 않은 사용자는 접근 불가")
    fun canManageGbsAttendance_UnauthenticatedHasNoAccess() {
        // given
        `when`(authentication.principal).thenReturn(null)
        
        // when
        val result = methodSecurityExpressions.canManageGbsAttendance(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canAccessVillage - 관리자는 모든 마을에 접근 가능")
    fun canAccessVillage_AdminHasAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(adminUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canAccessVillage - 교역자는 모든 마을에 접근 가능")
    fun canAccessVillage_MinisterHasAccess() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(ministerUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("canAccessVillage - 마을장은 자신의 마을에만 접근 가능")
    fun canAccessVillage_VillageLeaderHasAccessToOwnVillage() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(villageLeaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
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
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(villageLeaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
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
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(leaderUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("canAccessVillage - 인증되지 않은 사용자는 접근 불가")
    fun canAccessVillage_UnauthenticatedHasNoAccess() {
        // given
        `when`(authentication.principal).thenReturn(null)
        
        // when
        val result = methodSecurityExpressions.canAccessVillage(1L)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("isMinister - 교역자 권한 확인")
    fun isMinister_WithMinisterRole() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(ministerUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertTrue(result)
    }

    @Test
    @DisplayName("isMinister - 교역자가 아닌 경우")
    fun isMinister_WithNonMinisterRole() {
        // given
        userDetailsAdapter = mock(UserDetailsAdapter::class.java)
        `when`(userDetailsAdapter.getUser()).thenReturn(adminUser)
        `when`(authentication.principal).thenReturn(userDetailsAdapter)
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertFalse(result)
    }

    @Test
    @DisplayName("isMinister - 인증되지 않은 사용자")
    fun isMinister_WithUnauthenticatedUser() {
        // given
        `when`(authentication.principal).thenReturn(null)
        
        // when
        val result = methodSecurityExpressions.isMinister(authentication)
        
        // then
        assertFalse(result)
    }
} 