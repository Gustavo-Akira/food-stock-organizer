package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class UserJpaEntityTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val entity = UserJpaEntity(
            id = id,
            name = "Alice",
            email = "alice@example.com",
            passwordHash = "hashed_password"
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals("Alice", domain.name)
        assertEquals("alice@example.com", domain.email)
        assertEquals("hashed_password", domain.passwordHash)
    }

    @Test
    fun `fromDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val user = User(
            id = id,
            name = "Bob",
            email = "bob@example.com",
            passwordHash = "another_hash"
        )

        val entity = UserJpaEntity.fromDomain(user)

        assertEquals(id, entity.id)
        assertEquals("Bob", entity.name)
        assertEquals("bob@example.com", entity.email)
        assertEquals("another_hash", entity.passwordHash)
    }
}
