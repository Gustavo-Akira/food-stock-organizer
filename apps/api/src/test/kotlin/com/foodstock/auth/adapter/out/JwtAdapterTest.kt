package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtAdapterTest {

    private val secret = "test-secret-key-that-is-at-least-32-characters-long"
    private val adapter = JwtAdapter(secret, expirationMs = 3_600_000)
    private val user = User(
        id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        name = "Test User",
        email = "test@example.com",
        passwordHash = "hash"
    )

    @Test
    fun `extractUserId returns the userId embedded in the token`() {
        val token = adapter.generateToken(user)
        val result = adapter.extractUserId(token)
        assertEquals(user.id, result)
    }
}
