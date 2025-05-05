package com.attendly

import com.attendly.security.JwtAuthenticationFilter
import com.attendly.security.JwtTokenProvider
import com.attendly.security.TestSecurityConfig
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
class ApplicationTests {

	@MockkBean
	private lateinit var jwtTokenProvider: JwtTokenProvider

	@MockkBean
	private lateinit var userDetailsService: UserDetailsService

	@MockkBean
	private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

	@MockkBean
	private lateinit var authenticationManager: AuthenticationManager

	@Test
	fun contextLoads() {
	}

}
