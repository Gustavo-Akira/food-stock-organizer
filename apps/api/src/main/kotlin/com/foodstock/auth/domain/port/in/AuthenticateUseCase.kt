package com.foodstock.auth.domain.port.`in`

data class AuthenticateCommand(
    val email: String,
    val password: String
)

data class AuthToken(
    val token: String,
    val expiresIn: Long
)

interface AuthenticateUseCase {
    fun authenticate(command: AuthenticateCommand): AuthToken
}
