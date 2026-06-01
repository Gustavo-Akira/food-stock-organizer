package com.foodstock.auth.domain.port.out

import com.foodstock.auth.domain.model.User

interface PasswordHashPort {
    fun hash(raw: String): String
    fun matches(raw: String, hashed: String): Boolean
}

interface JwtPort {
    fun generateToken(user: User): String
    fun validateToken(token: String): Boolean
    fun extractEmail(token: String): String
}
