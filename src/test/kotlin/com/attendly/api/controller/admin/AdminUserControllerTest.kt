package com.attendly.api.controller.admin

import com.attendly.api.dto.*
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import com.attendly.service.AdminUserService
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(SpringExtension::class)
class AdminUserControllerTest {

    private lateinit var controller: AdminUserController
    private lateinit var adminUserService: AdminUserService
    
    @BeforeEach
    fun setup() {
        adminUserService = mockk()
        controller = AdminUserController(adminUserService)
    }

    @Test
    fun `createUser should return 201 and created user`() {
        // given
        val request = UserCreateRequest(
            name = "홍길동",
            email = "hong@example.com",
            password = "Password123!",
            role = Role.LEADER,
            departmentId = 1L
        )
        
        val response = UserResponse(
            id = 1L,
            name = "홍길동",
            email = "hong@example.com",
            phoneNumber = "010-1234-5678",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.createUser(any()) } returns response
        
        // when
        val result = controller.createUser(request)
        
        // then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertEquals(response, result.body?.data)
        
        verify { adminUserService.createUser(any()) }
    }

    @Test
    fun `updateUser should return 200 and updated user`() {
        // given
        val userId = 1L
        val request = UserUpdateRequest(
            name = "홍길동(수정)",
            email = "hong_updated@example.com",
            role = Role.ADMIN
        )
        
        val response = UserResponse(
            id = userId,
            name = "홍길동(수정)",
            email = "hong_updated@example.com",
            phoneNumber = "010-1234-5678",
            role = Role.ADMIN,
            status = UserStatus.ACTIVE,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.updateUser(userId, any()) } returns response
        
        // when
        val result = controller.updateUser(userId, request)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(response, result.body?.data)
        
        verify { adminUserService.updateUser(userId, any()) }
    }

    @Test
    fun `deleteUser should return 204`() {
        // given
        val userId = 1L
        
        // mockkBean의 메소드가 void를 반환하는 경우 justRun 사용
        justRun { adminUserService.deleteUser(userId) }
        
        // when
        val result = controller.deleteUser(userId)
        
        // then
        assertEquals(HttpStatus.NO_CONTENT, result.statusCode)
        
        verify { adminUserService.deleteUser(userId) }
    }

    @Test
    fun `getUser should return 200 and user information`() {
        // given
        val userId = 1L
        val response = UserResponse(
            id = userId,
            name = "홍길동",
            email = "hong@example.com",
            phoneNumber = "010-1234-5678",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.getUser(userId) } returns response
        
        // when
        val result = controller.getUser(userId)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertEquals(response, result.body?.data)
        
        verify { adminUserService.getUser(userId) }
    }

    @Test
    fun `getAllUsers should return 200 and paged list of users`() {
        // given
        val pageable = PageRequest.of(0, 10)
        val users = listOf(
            UserResponse(
                id = 1L,
                name = "홍길동",
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                role = Role.LEADER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "청년부",
                birthDate = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            UserResponse(
                id = 2L,
                name = "김철수",
                email = "kim@example.com",
                phoneNumber = "010-9876-5432",
                role = Role.MEMBER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "청년부",
                birthDate = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        val pagedResponse = PageImpl(users, pageable, 2)
        
        every { adminUserService.searchUsers(null, null, null, pageable) } returns pagedResponse
        
        // when
        val result = controller.getAllUsers(null, null, null, pageable)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertNotNull(result.body?.data)
        assertEquals(users, result.body?.data?.items)
        assertEquals(2L, result.body?.data?.totalCount)
        
        verify { adminUserService.searchUsers(null, null, null, pageable) }
    }

    @Test
    fun `getAllUsers with name parameter should return filtered users`() {
        // given
        val pageable = PageRequest.of(0, 10)
        val searchName = "홍길동"
        val users = listOf(
            UserResponse(
                id = 1L,
                name = "홍길동",
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                role = Role.LEADER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "청년부",
                birthDate = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        val pagedResponse = PageImpl(users, pageable, 1)
        
        every { adminUserService.searchUsers(searchName, null, null, pageable) } returns pagedResponse
        
        // when
        val result = controller.getAllUsers(searchName, null, null, pageable)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertNotNull(result.body?.data)
        assertEquals(users, result.body?.data?.items)
        assertEquals(1L, result.body?.data?.totalCount)
        
        verify { adminUserService.searchUsers(searchName, null, null, pageable) }
    }

    @Test
    fun `getAllUsers with all filters should return filtered users`() {
        // given
        val pageable = PageRequest.of(0, 10)
        val searchName = "홍길동"
        val departmentId = 1L
        val roles = listOf(Role.LEADER)
        val users = listOf(
            UserResponse(
                id = 1L,
                name = "홍길동",
                email = "hong@example.com",
                phoneNumber = "010-1234-5678",
                role = Role.LEADER,
                status = UserStatus.ACTIVE,
                departmentId = 1L,
                departmentName = "청년부",
                birthDate = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        val pagedResponse = PageImpl(users, pageable, 1)
        
        every { adminUserService.searchUsers(searchName, departmentId, roles, pageable) } returns pagedResponse
        
        // when
        val result = controller.getAllUsers(searchName, departmentId, roles, pageable)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        assertNotNull(result.body)
        assertNotNull(result.body?.data)
        assertEquals(users, result.body?.data?.items)
        assertEquals(1L, result.body?.data?.totalCount)
        
        verify { adminUserService.searchUsers(searchName, departmentId, roles, pageable) }
    }

    @Test
    fun `resetPassword should return 200`() {
        // given
        val userId = 1L
        val request = UserPasswordResetRequest(newPassword = "NewPassword123!")
        
        // mockkBean의 메소드가 void를 반환하는 경우 justRun 사용
        justRun { adminUserService.resetPassword(userId, any()) }
        
        // when
        val result = controller.resetPassword(userId, request)
        
        // then
        assertEquals(HttpStatus.OK, result.statusCode)
        
        verify { adminUserService.resetPassword(userId, any()) }
    }

    @Test
    fun `bulkCreateUsers should return 201 and creation summary`() {
        // given
        val request = UserBulkCreateRequest(
            users = listOf(
                UserCreateRequest(
                    name = "홍길동",
                    email = "hong@example.com",
                    password = "Password123!",
                    role = Role.LEADER,
                    departmentId = 1L
                ),
                UserCreateRequest(
                    name = "김철수",
                    email = "kim@example.com",
                    password = "Password123!",
                    role = Role.MEMBER,
                    departmentId = 1L
                )
            )
        )
        
        val response = UserBulkCreateResponse(
            createdCount = 2,
            failedCount = 0,
            failedEmails = emptyList()
        )
        
        every { adminUserService.bulkCreateUsers(any()) } returns response
        
        // when
        val result = controller.bulkCreateUsers(request)
        
        // then
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertEquals(response, result.body?.data)
        assertEquals(2, result.body?.data?.createdCount)
        assertEquals(0, result.body?.data?.failedCount)
        
        verify { adminUserService.bulkCreateUsers(any()) }
    }
} 