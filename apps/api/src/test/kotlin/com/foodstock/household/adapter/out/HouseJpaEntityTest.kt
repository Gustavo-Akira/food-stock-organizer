package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.House
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HouseJpaEntityTest {

    @Test
    fun `toDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        val updatedAt = LocalDateTime.of(2026, 1, 2, 12, 0)
        val entity = HouseJpaEntity(
            id = id,
            name = "Casa do Gustavo",
            ownerId = ownerId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals("Casa do Gustavo", domain.name)
        assertEquals(ownerId, domain.ownerId)
        assertEquals(createdAt, domain.createdAt)
        assertEquals(updatedAt, domain.updatedAt)
    }

    @Test
    fun `fromDomain maps all fields correctly`() {
        val id = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 3, 15, 8, 30)
        val updatedAt = LocalDateTime.of(2026, 3, 16, 9, 0)
        val house = House(
            id = id,
            name = "Casa da Maria",
            ownerId = ownerId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val entity = HouseJpaEntity.fromDomain(house)

        assertEquals(id, entity.id)
        assertEquals("Casa da Maria", entity.name)
        assertEquals(ownerId, entity.ownerId)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }
}
