package com.church.attendly.api.controller.admin

import com.church.attendly.api.dto.*
import com.church.attendly.domain.entity.Role
import com.church.attendly.security.JwtTokenProvider
import com.church.attendly.security.TestSecurityConfig
import com.church.attendly.service.AdminUserService
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminUserController::class)
@Import(TestSecurityConfig::class)
class AdminUserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @MockkBean
    private lateinit var adminUserService: AdminUserService
    
    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider
    
    @MockkBean
    private lateinit var authenticationManager: AuthenticationManager
    
    @MockkBean
    private lateinit var userDetailsService: UserDetailsService
    
    private val adminUser = User(
        "admin@example.com", 
        "password", 
        listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
    )

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
            role = Role.LEADER,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.createUser(any()) } returns response
        
        // when, then
        mockMvc.perform(
            post("/api/admin/users")
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
            .andExpect(jsonPath("$.role").value("LEADER"))
            .andExpect(jsonPath("$.departmentId").value(1))
            .andExpect(jsonPath("$.departmentName").value("청년부"))
        
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
            role = Role.ADMIN,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.updateUser(userId, any()) } returns response
        
        // when, then
        mockMvc.perform(
            put("/api/admin/users/$userId")
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("홍길동(수정)"))
            .andExpect(jsonPath("$.email").value("hong_updated@example.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
        
        verify { adminUserService.updateUser(userId, any()) }
    }

    @Test
    fun `deleteUser should return 204`() {
        // given
        val userId = 1L
        
        // mockkBean의 메소드가 void를 반환하는 경우 justRun 사용
        justRun { adminUserService.deleteUser(userId) }
        
        // when, then
        mockMvc.perform(
            delete("/api/admin/users/$userId")
                .with(user(adminUser))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNoContent)
        
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
            role = Role.LEADER,
            departmentId = 1L,
            departmentName = "청년부",
            birthDate = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { adminUserService.getUser(userId) } returns response
        
        // when, then
        mockMvc.perform(
            get("/api/admin/users/$userId")
                .with(user(adminUser))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("홍길동"))
            .andExpect(jsonPath("$.email").value("hong@example.com"))
            .andExpect(jsonPath("$.role").value("LEADER"))
        
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
                role = Role.LEADER,
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
                role = Role.MEMBER,
                departmentId = 1L,
                departmentName = "청년부",
                birthDate = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        val pagedResponse = PageImpl(users, pageable, 2)
        
        every { adminUserService.getAllUsers(any()) } returns pagedResponse
        
        // when, then
        mockMvc.perform(
            get("/api/admin/users")
                .with(user(adminUser))
                .param("page", "0")
                .param("size", "10")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].name").value("홍길동"))
            .andExpect(jsonPath("$.content[1].id").value(2))
            .andExpect(jsonPath("$.content[1].name").value("김철수"))
            .andExpect(jsonPath("$.totalElements").value(2))
        
        verify { adminUserService.getAllUsers(any()) }
    }

    @Test
    fun `resetPassword should return 200`() {
        // given
        val userId = 1L
        val request = UserPasswordResetRequest(newPassword = "NewPassword123!")
        
        // mockkBean의 메소드가 void를 반환하는 경우 justRun 사용
        justRun { adminUserService.resetPassword(userId, any()) }
        
        // when, then
        mockMvc.perform(
            post("/api/admin/users/$userId/reset-password")
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
        
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
        
        // when, then
        mockMvc.perform(
            post("/api/admin/users/bulk")
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.createdCount").value(2))
            .andExpect(jsonPath("$.failedCount").value(0))
            .andExpect(jsonPath("$.failedEmails.length()").value(0))
        
        verify { adminUserService.bulkCreateUsers(any()) }
    }
} 