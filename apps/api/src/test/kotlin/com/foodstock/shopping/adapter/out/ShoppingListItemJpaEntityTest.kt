package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingListItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class ShoppingListItemJpaEntityTest {

    @Test
    fun `toDomain and fromDomain round-trip with inventoryItemId populated`() {
        val id = UUID.randomUUID()
        val shoppingListId = UUID.randomUUID()
        val inventoryItemId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 1, 8, 0)
        val entity = ShoppingListItemJpaEntity(
            id = id,
            shoppingListId = shoppingListId,
            inventoryItemId = inventoryItemId,
            name = "Arroz",
            quantity = 2,
            checked = false,
            createdAt = createdAt
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals(shoppingListId, domain.shoppingListId)
        assertEquals(inventoryItemId, domain.inventoryItemId)
        assertEquals("Arroz", domain.name)
        assertEquals(2, domain.quantity)
        assertEquals(false, domain.checked)
        assertEquals(createdAt, domain.createdAt)

        val roundTripped = ShoppingListItemJpaEntity.fromDomain(domain)
        assertEquals(inventoryItemId, roundTripped.inventoryItemId)
        assertEquals("Arroz", roundTripped.name)
    }

    @Test
    fun `toDomain and fromDomain round-trip with inventoryItemId null`() {
        val id = UUID.randomUUID()
        val shoppingListId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 2, 1, 10, 0)
        val entity = ShoppingListItemJpaEntity(
            id = id,
            shoppingListId = shoppingListId,
            inventoryItemId = null,
            name = "Pão de Forma",
            quantity = 1,
            checked = true,
            createdAt = createdAt
        )

        val domain = entity.toDomain()

        assertNull(domain.inventoryItemId)
        assertEquals("Pão de Forma", domain.name)
        assertEquals(1, domain.quantity)
        assertEquals(true, domain.checked)

        val roundTripped = ShoppingListItemJpaEntity.fromDomain(domain)
        assertNull(roundTripped.inventoryItemId)
    }

    @Test
    fun `fromDomain preserves id, shoppingListId, quantity, checked and createdAt`() {
        val id = UUID.randomUUID()
        val shoppingListId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 3, 1, 10, 0)
        val item = ShoppingListItem(
            id = id, shoppingListId = shoppingListId, inventoryItemId = null,
            name = "Óleo", quantity = 3, checked = true, createdAt = createdAt
        )

        val entity = ShoppingListItemJpaEntity.fromDomain(item)

        assertEquals(id, entity.id)
        assertEquals(shoppingListId, entity.shoppingListId)
        assertEquals(3, entity.quantity)
        assertEquals(true, entity.checked)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    fun `constructor defaults all optional fields when only createdAt is specified`() {
        val createdAt = LocalDateTime.of(2026, 4, 1, 0, 0)
        val entity = ShoppingListItemJpaEntity(createdAt = createdAt)

        assertNotNull(entity.id)
        assertNotNull(entity.shoppingListId)
        assertNull(entity.inventoryItemId)
        assertEquals("", entity.name)
        assertEquals(1, entity.quantity)
        assertEquals(false, entity.checked)
        assertEquals(createdAt, entity.createdAt)
    }
}
