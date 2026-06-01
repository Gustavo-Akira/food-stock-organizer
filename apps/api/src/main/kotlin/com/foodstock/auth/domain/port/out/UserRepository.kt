package com.foodstock.auth.domain.port.out

import com.foodstock.auth.domain.model.User
import java.util.UUID

interface UserRepository {
    fun save(user: User): User
    fun findByEmail(email: String): User?
    fun findById(id: UUID): User?
    fun existsByEmail(email: String): Boolean
}
