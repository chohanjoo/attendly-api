package com.attendly.security

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class CustomUserDetailsServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val userDetailsService = CustomUserDetailsService(userRepository)

    @Test
    fun `loadUserByUsername 메서드는 존재하는 이메일로 사용자를 찾으면 UserDetailsAdapter를 반환한다`() {
        // given
        val email = "test@example.com"
        val department = mockk<Department>()
        val user = User(
            id = 1L,
            name = "테스트 사용자",
            birthDate = LocalDate.of(1990, 1, 1),
            role = Role.MEMBER,
            email = email,
            password = "encoded_password",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { userRepository.findByEmail(email) } returns Optional.of(user)

        // when
        val userDetails = userDetailsService.loadUserByUsername(email)

        // then
        verify(exactly = 1) { userRepository.findByEmail(email) }
        assertEquals(email, userDetails.username)
        assertEquals("encoded_password", userDetails.password)
        assertEquals(1, userDetails.authorities.size)
        assertEquals("ROLE_MEMBER", userDetails.authorities.first().authority)
        
        // UserDetailsAdapter 타입 확인 및 ID 검증
        val userDetailsAdapter = userDetails as UserDetailsAdapter
        assertEquals(1L, userDetailsAdapter.getId())
        assertEquals(user, userDetailsAdapter.getUser())
    }

    @Test
    fun `loadUserByUsername 메서드는 존재하지 않는 이메일로 조회하면 UsernameNotFoundException을 발생시킨다`() {
        // given
        val email = "nonexistent@example.com"
        every { userRepository.findByEmail(email) } returns Optional.empty()

        // when & then
        val exception = assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(email)
        }
        
        assertEquals("User not found with email: $email", exception.message)
        verify(exactly = 1) { userRepository.findByEmail(email) }
    }
} 