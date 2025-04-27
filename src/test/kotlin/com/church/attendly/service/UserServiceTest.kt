package com.church.attendly.service

import com.church.attendly.api.dto.SignupRequest
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.repository.DepartmentRepository
import com.church.attendly.domain.repository.UserRepository
import com.church.attendly.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var departmentRepository: DepartmentRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var userService: UserService

    @Captor
    private lateinit var userCaptor: ArgumentCaptor<User>

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

        `when`(userRepository.findByEmail(signupRequest.email)).thenReturn(Optional.empty())
        `when`(departmentRepository.findById(signupRequest.departmentId)).thenReturn(Optional.of(department))
        `when`(passwordEncoder.encode(signupRequest.password)).thenReturn(encodedPassword)
        `when`(userRepository.save(any(User::class.java))).thenReturn(savedUser)

        // When
        val response = userService.signup(signupRequest)

        // Then
        verify(userRepository).findByEmail(signupRequest.email)
        verify(departmentRepository).findById(signupRequest.departmentId)
        verify(passwordEncoder).encode(signupRequest.password)
        verify(userRepository).save(userCaptor.capture())

        val capturedUser = userCaptor.value
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

        `when`(userRepository.findByEmail(signupRequest.email)).thenReturn(Optional.of(existingUser))

        // When & Then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("이미 사용 중인 이메일입니다", exception.message)
        verify(userRepository).findByEmail(signupRequest.email)
        verify(departmentRepository, never()).findById(anyLong())
        verify(passwordEncoder, never()).encode(anyString())
        verify(userRepository, never()).save(any(User::class.java))
    }

    @Test
    fun `존재하지 않는 부서 ID로 회원가입 시도 시 예외 발생`() {
        // Given
        `when`(userRepository.findByEmail(signupRequest.email)).thenReturn(Optional.empty())
        `when`(departmentRepository.findById(signupRequest.departmentId)).thenReturn(Optional.empty())

        // When & Then
        val exception = assertThrows(ResourceNotFoundException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("찾을 수 없는 부서입니다: ID ${signupRequest.departmentId}", exception.message)
        verify(userRepository).findByEmail(signupRequest.email)
        verify(departmentRepository).findById(signupRequest.departmentId)
        verify(passwordEncoder, never()).encode(anyString())
        verify(userRepository, never()).save(any(User::class.java))
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
        `when`(userRepository.findByEmail(email)).thenReturn(Optional.of(user))

        // When
        val result = userService.findByEmail(email)

        // Then
        assertTrue(result.isPresent)
        assertEquals(user, result.get())
        verify(userRepository).findByEmail(email)
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
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(user))

        // When
        val result = userService.findById(userId)

        // Then
        assertTrue(result.isPresent)
        assertEquals(user, result.get())
        verify(userRepository).findById(userId)
    }
} 