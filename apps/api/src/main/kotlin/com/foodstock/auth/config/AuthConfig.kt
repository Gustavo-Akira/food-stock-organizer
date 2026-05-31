package com.foodstock.auth.config

import com.foodstock.auth.adapter.out.BcryptPasswordHashAdapter
import com.foodstock.auth.adapter.out.JwtAdapter
import com.foodstock.auth.adapter.out.UserJpaRepository
import com.foodstock.auth.domain.service.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthConfig(
    private val userJpaRepository: UserJpaRepository,
    private val passwordHashAdapter: BcryptPasswordHashAdapter,
    private val jwtAdapter: JwtAdapter
) {
    @Bean
    fun authService(): AuthService = AuthService(
        userRepository = userJpaRepository,
        passwordHashPort = passwordHashAdapter,
        jwtPort = jwtAdapter
    )
}
