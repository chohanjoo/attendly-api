package com.church.attendly.service

import com.church.attendly.api.dto.SignupRequest
import com.church.attendly.api.dto.UserListByRolesRequest
import com.church.attendly.api.dto.UserResponse
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.repository.DepartmentRepository
import com.church.attendly.domain.repository.UserRepository
import com.church.attendly.exception.ResourceNotFoundException
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var departmentRepository: DepartmentRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMockKs
    private lateinit var userService: UserService

    private lateinit var department: Department
    private lateinit var signupRequest: SignupRequest

    @BeforeEach
    fun setUp() {
        // 테스트용 부서 설정
        department = Department(
            id = 1L,
            name = "대학부"
        )

        // 테스트용 회원가입 요청 설정
        signupRequest = SignupRequest(
            email = "test@example.com",
            password = "password123",
            name = "홍길동",
            role = Role.LEADER,
            departmentId = 1L
        )
    }

    @Test
    fun `회원가입 성공 테스트`() {
        // Given
        val encodedPassword = "encoded_password"
        val savedUser = User(
            id = 1L,
            name = signupRequest.name,
            email = signupRequest.email,
            password = encodedPassword,
            role = signupRequest.role,
            department = department
        )

        every { userRepository.findByEmail(signupRequest.email) } returns Optional.empty()
        every { departmentRepository.findById(signupRequest.departmentId) } returns Optional.of(department)
        every { passwordEncoder.encode(signupRequest.password) } returns encodedPassword
        every { userRepository.save(any()) } returns savedUser

        // When
        val response = userService.signup(signupRequest)

        // Then
        verify { userRepository.findByEmail(signupRequest.email) }
        verify { departmentRepository.findById(signupRequest.departmentId) }
        verify { passwordEncoder.encode(signupRequest.password) }
        
        val userSlot = slot<User>()
        verify { userRepository.save(capture(userSlot)) }
        
        val capturedUser = userSlot.captured
        assertEquals(signupRequest.name, capturedUser.name)
        assertEquals(signupRequest.email, capturedUser.email)
        assertEquals(encodedPassword, capturedUser.password)
        assertEquals(signupRequest.role, capturedUser.role)
        assertEquals(department, capturedUser.department)

        assertEquals(1L, response.userId)
        assertEquals(signupRequest.name, response.name)
        assertEquals(signupRequest.email, response.email)
        assertEquals(signupRequest.role.name, response.role)
    }

    @Test
    fun `이미 존재하는 이메일로 회원가입 시도 시 예외 발생`() {
        // Given
        val existingUser = User(
            id = 2L,
            name = "기존 사용자",
            email = signupRequest.email,
            password = "existing_password",
            role = Role.MEMBER,
            department = department
        )

        every { userRepository.findByEmail(signupRequest.email) } returns Optional.of(existingUser)

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("이미 사용 중인 이메일입니다", exception.message)
        verify { userRepository.findByEmail(signupRequest.email) }
        verify(exactly = 0) { departmentRepository.findById(any()) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `존재하지 않는 부서 ID로 회원가입 시도 시 예외 발생`() {
        // Given
        every { userRepository.findByEmail(signupRequest.email) } returns Optional.empty()
        every { departmentRepository.findById(signupRequest.departmentId) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("찾을 수 없는 부서입니다: ID ${signupRequest.departmentId}", exception.message)
        verify { userRepository.findByEmail(signupRequest.email) }
        verify { departmentRepository.findById(signupRequest.departmentId) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `이메일로 사용자 찾기 테스트`() {
        // Given
        val email = "test@example.com"
        val user = User(
            id = 1L,
            name = "홍길동",
            email = email,
            password = "encoded_password",
            role = Role.LEADER,
            department = department
        )
        every { userRepository.findByEmail(email) } returns Optional.of(user)

        // When
        val result = userService.findByEmail(email)

        // Then
        assertTrue(result.isPresent)
        assertEquals(user, result.get())
        verify { userRepository.findByEmail(email) }
    }

    @Test
    fun `ID로 사용자 찾기 테스트`() {
        // Given
        val userId = 1L
        val user = User(
            id = userId,
            name = "홍길동",
            email = "test@example.com",
            password = "encoded_password",
            role = Role.LEADER,
            department = department
        )
        every { userRepository.findById(userId) } returns Optional.of(user)

        // When
        val result = userService.findById(userId)

        // Then
        assertTrue(result.isPresent)
        assertEquals(user, result.get())
        verify { userRepository.findById(userId) }
    }

    @Test
    fun `getUsersByRoles should return users with specified roles`() {
        // Given
        val department = Department(id = 1L, name = "IT 부서")
        val now = LocalDateTime.now()
        
        val leaderUser = User(
            id = 1L,
            name = "Leader User",
            email = "leader@example.com",
            password = "encoded_password",
            role = Role.LEADER,
            birthDate = LocalDate.of(1990, 1, 1),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        val memberUser = User(
            id = 2L,
            name = "Member User",
            email = "member@example.com",
            password = "encoded_password",
            role = Role.MEMBER,
            birthDate = LocalDate.of(1995, 5, 5),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        val adminUser = User(
            id = 3L,
            name = "Admin User",
            email = "admin@example.com",
            password = "encoded_password",
            role = Role.ADMIN,
            birthDate = LocalDate.of(1985, 10, 10),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        every { userRepository.findByRole(Role.LEADER) } returns listOf(leaderUser)
        every { userRepository.findByRole(Role.MEMBER) } returns listOf(memberUser)
        
        val request = UserListByRolesRequest(roles = listOf("LEADER", "MEMBER"))
        
        // When
        val result = userService.getUsersByRoles(request)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("Leader User", result[0].name)
        assertEquals(Role.LEADER, result[0].role)
        assertEquals("Member User", result[1].name)
        assertEquals(Role.MEMBER, result[1].role)
        
        verify { userRepository.findByRole(Role.LEADER) }
        verify { userRepository.findByRole(Role.MEMBER) }
    }
    
    @Test
    fun `getUsersByRoles should return empty list when roles list is empty`() {
        // Given
        val request = UserListByRolesRequest(roles = emptyList())
        
        // When
        val result = userService.getUsersByRoles(request)
        
        // Then
        assertEquals(0, result.size)
    }
    
    @Test
    fun `getUsersByRoles should filter out duplicate users`() {
        // Given
        val department = Department(id = 1L, name = "IT 부서")
        val now = LocalDateTime.now()
        
        val leaderUser = User(
            id = 1L,
            name = "Leader User",
            email = "leader@example.com",
            password = "encoded_password",
            role = Role.LEADER,
            birthDate = LocalDate.of(1990, 1, 1),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        // User appears in both role queries (this is a test case, though it shouldn't happen in real data)
        every { userRepository.findByRole(Role.LEADER) } returns listOf(leaderUser)
        every { userRepository.findByRole(Role.VILLAGE_LEADER) } returns listOf(leaderUser)
        
        val request = UserListByRolesRequest(roles = listOf("LEADER", "VILLAGE_LEADER"))
        
        // When
        val result = userService.getUsersByRoles(request)
        
        // Then
        assertEquals(1, result.size) // Only one user despite being in two roles
        assertEquals("Leader User", result[0].name)
        
        verify { userRepository.findByRole(Role.LEADER) }
        verify { userRepository.findByRole(Role.VILLAGE_LEADER) }
    }
} 