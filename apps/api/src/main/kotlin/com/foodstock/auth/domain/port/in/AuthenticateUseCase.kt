package com.foodstock.auth.domain.port.`in`

import com.foodstock.auth.domain.model.User

data class LoginResult(val token: String, val user: User)

interface RegisterUseCase {
    fun register(name: String, email: String, password: String): User
}

interface LoginUseCase {
    fun login(email: String, password: String): LoginResult
}
