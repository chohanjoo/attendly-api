package com.attendly.service

import com.attendly.api.dto.SignupRequest
import com.attendly.api.dto.UserListByRolesRequest
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.enums.Role
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.entity.VillageLeader
import com.attendly.domain.repository.DepartmentRepository
import com.attendly.domain.repository.GbsMemberHistoryRepository
import com.attendly.domain.repository.UserRepository
import com.attendly.exception.AttendlyApiException
import com.attendly.exception.ErrorMessage
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.core.Authentication
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
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @MockK
    private lateinit var passwordEncoder: PasswordEncoder

    @MockK
    private lateinit var authentication: Authentication

    @InjectMockKs
    private lateinit var userService: UserService

    private val userId = 1L
    private val userName = "테스트 사용자"
    private val villageId = 10L
    private val villageName = "테스트 마을"
    private val departmentId = 20L
    private val departmentName = "테스트 부서"
    private val gbsId = 30L
    private val gbsName = "테스트 GBS"

    private lateinit var department: Department
    private lateinit var signupRequest: SignupRequest

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        
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
            phoneNumber = null,
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
            phoneNumber = null,
            role = Role.MEMBER,
            department = department
        )

        every { userRepository.findByEmail(signupRequest.email) } returns Optional.of(existingUser)

        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("이미 사용 중인 이메일입니다", exception.message)
        assertEquals(ErrorMessage.DUPLICATE_EMAIL, exception.errorMessage)
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
        val exception = assertThrows(AttendlyApiException::class.java) {
            userService.signup(signupRequest)
        }
        assertEquals("찾을 수 없는 부서입니다: ID ${signupRequest.departmentId}", exception.message)
        assertEquals(ErrorMessage.DEPARTMENT_NOT_FOUND, exception.errorMessage)
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
            phoneNumber = null,
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
            phoneNumber = null,
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
            phoneNumber = "010-1234-5678",
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
            phoneNumber = "010-9876-5432",
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
            phoneNumber = "010-5555-5555",
            role = Role.ADMIN,
            birthDate = LocalDate.of(1985, 10, 10),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        every { userRepository.findByRoles(listOf(Role.LEADER, Role.MEMBER)) } returns listOf(leaderUser, memberUser)
        
        val request = UserListByRolesRequest(roles = listOf("LEADER", "MEMBER"))
        
        // When
        val result = userService.getUsersByRoles(request)
        
        // Then
        assertEquals(2, result.size)
        assertEquals("Leader User", result[0].name)
        assertEquals(Role.LEADER, result[0].role)
        assertEquals("Member User", result[1].name)
        assertEquals(Role.MEMBER, result[1].role)
        
        verify { userRepository.findByRoles(listOf(Role.LEADER, Role.MEMBER)) }
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
    fun `getUsersByRoles should not return duplicate users from single query`() {
        // Given
        val department = Department(id = 1L, name = "IT 부서")
        val now = LocalDateTime.now()
        
        val leaderUser = User(
            id = 1L,
            name = "Leader User",
            email = "leader@example.com",
            password = "encoded_password",
            phoneNumber = "010-1234-5678",
            role = Role.LEADER,
            birthDate = LocalDate.of(1990, 1, 1),
            department = department,
            createdAt = now,
            updatedAt = now
        )
        
        // Single query returns only one user per role
        every { userRepository.findByRoles(listOf(Role.LEADER, Role.VILLAGE_LEADER)) } returns listOf(leaderUser)
        
        val request = UserListByRolesRequest(roles = listOf("LEADER", "VILLAGE_LEADER"))
        
        // When
        val result = userService.getUsersByRoles(request)
        
        // Then
        assertEquals(1, result.size) // Only one user returned
        assertEquals("Leader User", result[0].name)
        
        verify { userRepository.findByRoles(listOf(Role.LEADER, Role.VILLAGE_LEADER)) }
    }

    @Test
    fun `getCurrentUser should return user when authentication is valid`() {
        // Given
        val email = "test@example.com"
        val user = User(
            id = 1L,
            name = "홍길동",
            email = email,
            password = "encoded_password",
            phoneNumber = null,
            role = Role.LEADER,
            department = department
        )
        
        val authentication = mockk<org.springframework.security.core.Authentication>()
        every { authentication.name } returns email
        every { userRepository.findByEmail(email) } returns Optional.of(user)

        // When
        val result = userService.getCurrentUser(authentication)

        // Then
        assertEquals(user, result)
        verify { authentication.name }
        verify { userRepository.findByEmail(email) }
    }

    @Test
    fun `getCurrentUser should throw exception when user not found`() {
        // Given
        val email = "nonexistent@example.com"
        
        val authentication = mockk<org.springframework.security.core.Authentication>()
        every { authentication.name } returns email
        every { userRepository.findByEmail(email) } returns Optional.empty()

        // When & Then
        val exception = assertThrows(AttendlyApiException::class.java) {
            userService.getCurrentUser(authentication)
        }
        
        assertEquals("사용자를 찾을 수 없습니다", exception.message)
        assertEquals(ErrorMessage.USER_NOT_FOUND, exception.errorMessage)
        verify { authentication.name }
        verify { userRepository.findByEmail(email) }
    }

    @Test
    fun `마을장 사용자의 마을 정보 조회 성공`() {
        // given
        val department = Department(
            id = departmentId,
            name = departmentName
        )
        
        val village = Village(
            id = villageId,
            name = villageName,
            department = department
        )
        
        val villageLeader = VillageLeader(
            user = mockk(),
            village = village,
            startDate = LocalDate.now()
        )
        
        val user = User(
            id = userId,
            name = userName,
            role = Role.VILLAGE_LEADER,
            department = department,
            villageLeader = villageLeader
        )
        
        every { userRepository.findById(userId) } returns Optional.of(user)
        
        // when
        val result = userService.getUserVillage(userId)
        
        // then
        assertEquals(userId, result.userId)
        assertEquals(userName, result.userName)
        assertEquals(villageId, result.villageId)
        assertEquals(villageName, result.villageName)
        assertEquals(departmentId, result.departmentId)
        assertEquals(departmentName, result.departmentName)
        assertTrue(result.isVillageLeader)
        
        verify(exactly = 1) { userRepository.findById(userId) }
    }
    
    @Test
    fun `일반 사용자의 마을 정보 조회 성공`() {
        // given
        val department = Department(
            id = departmentId,
            name = departmentName
        )
        
        val village = Village(
            id = villageId,
            name = villageName,
            department = department
        )
        
        val user = User(
            id = userId,
            name = userName,
            role = Role.MEMBER,
            department = department
        )
        
        val gbsGroup = GbsGroup(
            id = gbsId,
            name = gbsName,
            village = village,
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6)
        )
        
        val memberHistory = GbsMemberHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            member = user,
            startDate = LocalDate.now()
        )
        
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) } returns memberHistory
        
        // when
        val result = userService.getUserVillage(userId)
        
        // then
        assertEquals(userId, result.userId)
        assertEquals(userName, result.userName)
        assertEquals(villageId, result.villageId)
        assertEquals(villageName, result.villageName)
        assertEquals(departmentId, result.departmentId)
        assertEquals(departmentName, result.departmentName)
        assertFalse(result.isVillageLeader)
        
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) }
    }
    
    @Test
    fun `사용자가 마을에 배정되지 않은 경우 예외 발생`() {
        // given
        val department = Department(
            id = departmentId,
            name = departmentName
        )
        
        val user = User(
            id = userId,
            name = userName,
            role = Role.MEMBER,
            department = department
        )
        
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) } returns null
        
        // when & then
        val exception = assertThrows(AttendlyApiException::class.java) {
            userService.getUserVillage(userId)
        }
        
        assertEquals(ErrorMessage.USER_NOT_ASSIGNED_TO_VILLAGE.code, exception.errorMessage.code)
        
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) }
    }
    
    @Test
    fun `현재 인증된 사용자의 마을 정보 조회 성공`() {
        // given
        val department = Department(
            id = departmentId,
            name = departmentName
        )
        
        val village = Village(
            id = villageId,
            name = villageName,
            department = department
        )
        
        val user = User(
            id = userId,
            name = userName,
            email = "test@example.com",
            role = Role.MEMBER,
            department = department
        )
        
        val gbsGroup = GbsGroup(
            id = gbsId,
            name = gbsName,
            village = village,
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6)
        )
        
        val memberHistory = GbsMemberHistory(
            id = 1L,
            gbsGroup = gbsGroup,
            member = user,
            startDate = LocalDate.now()
        )
        
        every { authentication.name } returns "test@example.com"
        every { userRepository.findByEmail("test@example.com") } returns Optional.of(user)
        every { userRepository.findById(userId) } returns Optional.of(user)
        every { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) } returns memberHistory
        
        // when
        val result = userService.getCurrentUserVillage(authentication)
        
        // then
        assertEquals(userId, result.userId)
        assertEquals(userName, result.userName)
        assertEquals(villageId, result.villageId)
        assertEquals(villageName, result.villageName)
        assertEquals(departmentId, result.departmentId)
        assertEquals(departmentName, result.departmentName)
        assertFalse(result.isVillageLeader)
        
        verify { authentication.name }
        verify { userRepository.findByEmail("test@example.com") }
        verify { userRepository.findById(userId) }
        verify { gbsMemberHistoryRepository.findCurrentMemberHistoryByMemberId(userId) }
    }
} 