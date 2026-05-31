package com.foodstock.auth.domain.service

import com.foodstock.auth.domain.port.`in`.AuthToken
import com.foodstock.auth.domain.port.`in`.AuthenticateCommand
import com.foodstock.auth.domain.port.`in`.AuthenticateUseCase
import com.foodstock.auth.domain.port.out.JwtPort
import com.foodstock.auth.domain.port.out.PasswordHashPort
import com.foodstock.auth.domain.port.out.UserRepository
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val jwtPort: JwtPort
) : AuthenticateUseCase {

    override fun authenticate(command: AuthenticateCommand): AuthToken {
        val user = userRepository.findByEmail(command.email)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordHashPort.matches(command.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        return jwtPort.generateToken(user.id, user.email)
    }
}
