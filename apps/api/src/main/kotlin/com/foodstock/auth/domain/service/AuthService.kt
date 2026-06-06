package com.foodstock.auth.domain.service

import com.foodstock.auth.domain.exception.EmailAlreadyInUseException
import com.foodstock.auth.domain.exception.InvalidCredentialsException
import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.`in`.LoginResult
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import com.foodstock.auth.domain.port.out.JwtPort
import com.foodstock.auth.domain.port.out.PasswordHashPort
import com.foodstock.auth.domain.port.out.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val jwtPort: JwtPort
) : RegisterUseCase, LoginUseCase {

    override fun register(name: String, email: String, password: String): User {
        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyInUseException(email)
        }
        val user = User(
            name = name,
            email = email,
            passwordHash = passwordHashPort.hash(password)
        )
        return userRepository.save(user)
    }

    override fun login(email: String, password: String): LoginResult {
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException()
        if (!passwordHashPort.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        val token = jwtPort.generateToken(user)
        return LoginResult(token = token, user = user)
    }
}
