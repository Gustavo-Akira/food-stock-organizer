package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class ShoppingListJpaEntityTest {

    @Test
    fun `toDomain maps all fields including status enum correctly`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        val updatedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val entity = ShoppingListJpaEntity(
            id = id,
            houseId = houseId,
            name = "Lista Semanal",
            status = ShoppingListStatus.OPEN,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals(houseId, domain.houseId)
        assertEquals("Lista Semanal", domain.name)
        assertEquals(ShoppingListStatus.OPEN, domain.status)
        assertEquals(createdBy, domain.createdBy)
        assertEquals(createdAt, domain.createdAt)
        assertEquals(updatedAt, domain.updatedAt)
    }

    @Test
    fun `fromDomain maps all fields including status enum correctly`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 5, 10, 14, 0)
        val updatedAt = LocalDateTime.of(2026, 5, 10, 16, 30)
        val list = ShoppingList(
            id = id,
            houseId = houseId,
            name = "Lista Concluída",
            status = ShoppingListStatus.COMPLETED,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val entity = ShoppingListJpaEntity.fromDomain(list)

        assertEquals(id, entity.id)
        assertEquals(houseId, entity.houseId)
        assertEquals("Lista Concluída", entity.name)
        assertEquals(ShoppingListStatus.COMPLETED, entity.status)
        assertEquals(createdBy, entity.createdBy)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }
}
