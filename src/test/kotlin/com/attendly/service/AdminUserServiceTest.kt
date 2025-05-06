package com.attendly.service

import com.attendly.api.dto.UserCreateRequest
import com.attendly.api.dto.UserPasswordResetRequest
import com.attendly.api.dto.UserUpdateRequest
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorCode
import com.attendly.exception.ErrorMessage
import com.attendly.exception.ErrorMessageUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.*

class AdminUserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var departmentRepository: DepartmentRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var adminUserService: AdminUserService

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
        departmentRepository = mockk(relaxed = true)
        passwordEncoder = mockk(relaxed = true)
        adminUserService = AdminUserService(userRepository, departmentRepository, passwordEncoder)
    }

    @Test
    fun `createUser should create and return a user with valid data`() {
        // given
        val department = Department(id = 1L, name = "청년부")
        val userCreateRequest = UserCreateRequest(
            name = "홍길동",
            email = "hong@example.com",
            password = "Password123!",
            role = Role.LEADER,
            departmentId = 1L
        )
        val encodedPassword = "encodedPassword"
        val createdUser = User(
            id = 1L,
            name = "홍길동",
            email = "hong@example.com",
            password = encodedPassword,
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every { departmentRepository.findById(1L) } returns Optional.of(department)
        every { userRepository.findByEmail("hong@example.com") } returns Optional.empty()
        every { passwordEncoder.encode("Password123!") } returns encodedPassword
        every { userRepository.save(any()) } returns createdUser

        // when
        val result = adminUserService.createUser(userCreateRequest)

        // then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("홍길동", result.name)
        assertEquals("hong@example.com", result.email)
        assertEquals(Role.LEADER, result.role)
        assertEquals(1L, result.departmentId)
        assertEquals("청년부", result.departmentName)
        
        verify { userRepository.findByEmail("hong@example.com") }
        verify { departmentRepository.findById(1L) }
        verify { passwordEncoder.encode("Password123!") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `createUser should throw exception when email already exists`() {
        // given
        val userCreateRequest = UserCreateRequest(
            name = "홍길동",
            email = "hong@example.com",
            password = "Password123!",
            role = Role.LEADER,
            departmentId = 1L
        )
        val existingUser = User(
            id = 2L,
            name = "기존사용자",
            email = "hong@example.com",
            password = "encodedPassword",
            role = Role.MEMBER,
            department = Department(id = 1L, name = "청년부")
        )

        every { userRepository.findByEmail("hong@example.com") } returns Optional.of(existingUser)

        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminUserService.createUser(userCreateRequest)
        }
        assertEquals(ErrorMessage.DUPLICATE_EMAIL.code, exception.errorCode)
        assertEquals(ErrorMessage.DUPLICATE_EMAIL.message, exception.message)
        
        verify { userRepository.findByEmail("hong@example.com") }
        verify(exactly = 0) { departmentRepository.findById(any()) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `updateUser should update and return user with valid data`() {
        // given
        val userId = 1L
        val userUpdateRequest = UserUpdateRequest(
            name = "홍길동(수정)",
            email = "hong_updated@example.com",
            role = Role.ADMIN
        )
        val department = Department(id = 1L, name = "청년부")
        val existingUser = User(
            id = userId,
            name = "홍길동",
            email = "hong@example.com",
            password = "encodedPassword",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now().minusDays(10),
            updatedAt = LocalDateTime.now().minusDays(10)
        )
        val updatedUser = User(
            id = userId,
            name = "홍길동(수정)",
            email = "hong_updated@example.com",
            password = "encodedPassword",
            role = Role.ADMIN,
            department = department,
            createdAt = existingUser.createdAt,
            updatedAt = LocalDateTime.now()
        )

        every { userRepository.findById(userId) } returns Optional.of(existingUser)
        every { userRepository.findByEmail("hong_updated@example.com") } returns Optional.empty()
        every { userRepository.save(any()) } returns updatedUser

        // when
        val result = adminUserService.updateUser(userId, userUpdateRequest)

        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("홍길동(수정)", result.name)
        assertEquals("hong_updated@example.com", result.email)
        assertEquals(Role.ADMIN, result.role)
        
        verify { userRepository.findById(userId) }
        verify { userRepository.findByEmail("hong_updated@example.com") }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `resetPassword should update user's password`() {
        // given
        val userId = 1L
        val passwordResetRequest = UserPasswordResetRequest(newPassword = "NewPassword123!")
        val department = Department(id = 1L, name = "청년부")
        val existingUser = User(
            id = userId,
            name = "홍길동",
            email = "hong@example.com",
            password = "oldEncodedPassword",
            role = Role.LEADER,
            department = department
        )
        val encodedNewPassword = "newEncodedPassword"
        val userSlot = slot<User>()
        val updatedUser = User(
            id = userId,
            name = "홍길동",
            email = "hong@example.com",
            password = encodedNewPassword,
            role = Role.LEADER,
            department = department
        )

        every { userRepository.findById(userId) } returns Optional.of(existingUser)
        every { passwordEncoder.encode("NewPassword123!") } returns encodedNewPassword
        every { userRepository.save(capture(userSlot)) } returns updatedUser

        // when
        adminUserService.resetPassword(userId, passwordResetRequest)

        // then
        verify { userRepository.findById(userId) }
        verify { passwordEncoder.encode("NewPassword123!") }
        verify { userRepository.save(any()) }
        
        assertEquals(encodedNewPassword, userSlot.captured.password)
    }

    @Test
    fun `deleteUser should delete user when exists`() {
        // given
        val userId = 1L
        
        every { userRepository.existsById(userId) } returns true
        
        // when
        adminUserService.deleteUser(userId)
        
        // then
        verify { userRepository.existsById(userId) }
        verify { userRepository.deleteById(userId) }
    }

    @Test
    fun `deleteUser should throw exception when user does not exist`() {
        // given
        val userId = 999L
        
        every { userRepository.existsById(userId) } returns false
        
        // when & then
        val exception = assertThrows<AttendlyApiException> {
            adminUserService.deleteUser(userId)
        }
        
        assertEquals(ErrorMessage.USER_NOT_FOUND.code, exception.errorCode)
        assertEquals(ErrorMessageUtils.withId(ErrorMessage.USER_NOT_FOUND, userId), exception.message)
        verify { userRepository.existsById(userId) }
        verify(exactly = 0) { userRepository.deleteById(any()) }
    }

    @Test
    fun `getUser should return user when exists`() {
        // given
        val userId = 1L
        val department = Department(id = 1L, name = "청년부")
        val user = User(
            id = userId,
            name = "홍길동",
            email = "hong@example.com",
            password = "encodedPassword",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { userRepository.findById(userId) } returns Optional.of(user)
        
        // when
        val result = adminUserService.getUser(userId)
        
        // then
        assertNotNull(result)
        assertEquals(userId, result.id)
        assertEquals("홍길동", result.name)
        assertEquals("hong@example.com", result.email)
        assertEquals(Role.LEADER, result.role)
        assertEquals(1L, result.departmentId)
        assertEquals("청년부", result.departmentName)
        
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getAllUsers should return paged list of users`() {
        // given
        val pageable = PageRequest.of(0, 10)
        val department = Department(id = 1L, name = "청년부")
        val users = listOf(
            User(
                id = 1L,
                name = "홍길동",
                email = "hong@example.com",
                password = "encodedPassword",
                role = Role.LEADER,
                department = department,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            User(
                id = 2L,
                name = "김철수",
                email = "kim@example.com",
                password = "encodedPassword",
                role = Role.MEMBER,
                department = department,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )
        
        every { userRepository.findAll(pageable) } returns PageImpl(users, pageable, 2)
        
        // when
        val result = adminUserService.getAllUsers(pageable)
        
        // then
        assertNotNull(result)
        assertEquals(2, result.totalElements)
        assertEquals(2, result.content.size)
        assertEquals(1L, result.content[0].id)
        assertEquals("홍길동", result.content[0].name)
        assertEquals(2L, result.content[1].id)
        assertEquals("김철수", result.content[1].name)
        
        verify { userRepository.findAll(pageable) }
    }
} 