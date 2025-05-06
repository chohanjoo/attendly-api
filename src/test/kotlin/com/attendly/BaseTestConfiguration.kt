package com.attendly

import com.attendly.config.P6spyConfig
import com.attendly.config.TestP6spyConfig
import com.attendly.security.JwtAuthenticationFilter
import com.attendly.security.JwtTokenProvider
import com.attendly.security.TestSecurityConfig
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(TestSecurityConfig::class, TestP6spyConfig::class)
@ActiveProfiles("test")
abstract class BaseTestConfiguration {
    
    @MockkBean
    protected lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    protected lateinit var userDetailsService: UserDetailsService

    @MockkBean
    protected lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    protected lateinit var authenticationManager: AuthenticationManager
    
    @MockkBean
    protected lateinit var p6spyConfig: P6spyConfig
} 