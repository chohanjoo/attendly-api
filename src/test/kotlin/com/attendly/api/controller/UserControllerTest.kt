package com.attendly.api.controller

import com.attendly.api.dto.UserListByRolesRequest
import com.attendly.api.dto.UserResponse
import com.attendly.api.dto.UserVillageResponse
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import com.attendly.service.UserService
import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(SpringExtension::class)
class UserControllerTest {

    private lateinit var controller: UserController
    private lateinit var userService: UserService
    private lateinit var authentication: Authentication

    @BeforeEach
    fun setup() {
        userService = mockk()
        authentication = mockk()
        
        controller = UserController(userService)
    }

    @Test
    fun `should return users filtered by roles`() {
        // Given
        val request = UserListByRolesRequest(roles = listOf("LEADER", "MEMBER"))
        val now = LocalDateTime.now()
        val users = listOf(
            UserResponse(
                id = 1L,
                name = "Leader User",
                email = "leader@example.com",
                phoneNumber = "010-1234-5678",
                role = Role.LEADER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "IT 부서",
                birthDate = LocalDate.of(1990, 1, 1),
                createdAt = now,
                updatedAt = now
            ),
            UserResponse(
                id = 2L,
                name = "Member User",
                email = "member@example.com",
                phoneNumber = "010-9876-5432",
                role = Role.MEMBER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "IT 부서",
                birthDate = LocalDate.of(1995, 5, 5),
                createdAt = now,
                updatedAt = now
            )
        )

        every { userService.getUsersByRoles(any()) } returns users

        // When
        val result = controller.getUsersByRoles(request)

        // Then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(2, responseData?.users?.size)
        assertEquals("Leader User", responseData?.users?.get(0)?.name)
        assertEquals(Role.LEADER, responseData?.users?.get(0)?.role)
        assertEquals("Member User", responseData?.users?.get(1)?.name)
        assertEquals(Role.MEMBER, responseData?.users?.get(1)?.role)

        verify { userService.getUsersByRoles(request) }
    }

    @Test
    fun `역할별 사용자 목록 조회 성공`() {
        // given
        val request = UserListByRolesRequest(listOf("LEADER", "MEMBER"))
        val usersList = listOf(
            UserResponse(
                id = 1L,
                name = "홍길동",
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                role = Role.LEADER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "대학부",
                birthDate = LocalDate.of(1990, 1, 1),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            UserResponse(
                id = 2L,
                name = "김철수",
                email = "kim@example.com",
                phoneNumber = "010-2345-6789",
                role = Role.MEMBER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "대학부",
                birthDate = LocalDate.of(1995, 5, 5),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        every { userService.getUsersByRoles(any()) } returns usersList

        // when
        val result = controller.getUsersByRoles(request)

        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(2, responseData?.users?.size)
        assertEquals(1L, responseData?.users?.get(0)?.id)
        assertEquals("홍길동", responseData?.users?.get(0)?.name)
        assertEquals(Role.LEADER, responseData?.users?.get(0)?.role)
        assertEquals(2L, responseData?.users?.get(1)?.id)
        assertEquals("김철수", responseData?.users?.get(1)?.name)
        assertEquals(Role.MEMBER, responseData?.users?.get(1)?.role)
    }
    
    @Test
    fun `현재 사용자의 마을 정보 조회 성공`() {
        // given
        val villageResponse = UserVillageResponse(
            userId = 1L,
            userName = "홍길동",
            villageId = 10L,
            villageName = "1마을",
            departmentId = 20L,
            departmentName = "대학부",
            isVillageLeader = false
        )
        
        every { userService.getCurrentUserVillage(authentication) } returns villageResponse
        
        // when
        val result = controller.getCurrentUserVillage(authentication)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertTrue(result.body?.success == true)
        
        val responseData = result.body?.data
        assertEquals(1L, responseData?.userId)
        assertEquals("홍길동", responseData?.userName)
        assertEquals(10L, responseData?.villageId)
        assertEquals("1마을", responseData?.villageName)
        assertEquals(20L, responseData?.departmentId)
        assertEquals("대학부", responseData?.departmentName)
        assertEquals(false, responseData?.isVillageLeader)
    }
} 