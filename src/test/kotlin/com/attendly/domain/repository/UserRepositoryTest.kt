package com.attendly.domain.repository

import com.attendly.config.TestQuerydslConfig
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.User
import com.attendly.enums.Role
import com.attendly.enums.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@Import(TestQuerydslConfig::class, UserRepositoryImpl::class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var department: Department
    private lateinit var leaderUser: User
    private lateinit var memberUser: User
    private lateinit var adminUser: User
    private lateinit var villageLeaderUser: User

    @BeforeEach
    fun setUp() {
        // 부서 생성
        department = Department(
            name = "청년부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(department)

        // 리더 사용자 생성
        leaderUser = User(
            name = "리더",
            email = "leader@example.com",
            password = "password",
            role = Role.LEADER,
            status = UserStatus.ACTIVE,
            department = department,
            birthDate = LocalDate.of(1990, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leaderUser)

        // 멤버 사용자 생성
        memberUser = User(
            name = "멤버",
            email = "member@example.com",
            password = "password",
            role = Role.MEMBER,
            status = UserStatus.ACTIVE,
            department = department,
            birthDate = LocalDate.of(1995, 5, 5),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(memberUser)

        // 관리자 사용자 생성
        adminUser = User(
            name = "관리자",
            email = "admin@example.com",
            password = "password",
            role = Role.ADMIN,
            status = UserStatus.ACTIVE,
            department = department,
            birthDate = LocalDate.of(1985, 3, 15),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(adminUser)

        // 마을장 사용자 생성
        villageLeaderUser = User(
            name = "마을장",
            email = "village_leader@example.com",
            password = "password",
            role = Role.VILLAGE_LEADER,
            status = UserStatus.ACTIVE,
            department = department,
            birthDate = LocalDate.of(1988, 7, 20),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(villageLeaderUser)

        entityManager.flush()
    }

    @Test
    fun `findByRoles should return users with specified roles`() {
        // given
        val roles = listOf(Role.LEADER, Role.MEMBER)

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertEquals(2, result.size)
        val userNames = result.map { it.name }
        assertTrue(userNames.contains("리더"))
        assertTrue(userNames.contains("멤버"))
    }

    @Test
    fun `findByRoles should return empty list when no roles provided`() {
        // given
        val roles = emptyList<Role>()

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByRoles should return users ordered by id`() {
        // given
        val roles = listOf(Role.LEADER, Role.ADMIN, Role.MEMBER)

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertEquals(3, result.size)
        // ID 순서대로 정렬되어야 함
        assertTrue(result[0].id!! < result[1].id!!)
        assertTrue(result[1].id!! < result[2].id!!)
    }

    @Test
    fun `findByRoles should return only users with exact roles`() {
        // given
        val roles = listOf(Role.VILLAGE_LEADER)

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertEquals(1, result.size)
        assertEquals("마을장", result[0].name)
        assertEquals(Role.VILLAGE_LEADER, result[0].role)
    }

    @Test
    fun `findByRoles should return empty list when no users match roles`() {
        // given
        val roles = listOf(Role.MINISTER)

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByRoles should return all users when all roles are specified`() {
        // given
        val roles = listOf(Role.LEADER, Role.MEMBER, Role.ADMIN, Role.VILLAGE_LEADER)

        // when
        val result = userRepository.findByRoles(roles)

        // then
        assertEquals(4, result.size)
        val userNames = result.map { it.name }
        assertTrue(userNames.contains("리더"))
        assertTrue(userNames.contains("멤버"))
        assertTrue(userNames.contains("관리자"))
        assertTrue(userNames.contains("마을장"))
    }
} 