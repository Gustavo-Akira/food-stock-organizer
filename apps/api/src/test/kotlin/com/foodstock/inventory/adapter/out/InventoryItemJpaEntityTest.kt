package com.foodstock.inventory.adapter.out

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InventoryItemJpaEntityTest {

    @Test
    fun `toDomain and fromDomain round-trip with all optional fields populated`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 1, 8, 0)
        val updatedAt = LocalDateTime.of(2026, 1, 2, 9, 0)
        val expiryDate = LocalDate.of(2026, 12, 31)
        val entity = InventoryItemJpaEntity(
            id = id,
            houseId = houseId,
            name = "Arroz",
            category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY,
            expiryDate = expiryDate,
            notes = "Comprar integral",
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals(houseId, domain.houseId)
        assertEquals("Arroz", domain.name)
        assertEquals(Category.FOOD, domain.category)
        assertEquals(QuantityLevel.PLENTY, domain.quantityLevel)
        assertEquals(expiryDate, domain.expiryDate)
        assertEquals("Comprar integral", domain.notes)
        assertEquals(createdAt, domain.createdAt)
        assertEquals(updatedAt, domain.updatedAt)

        val roundTripped = InventoryItemJpaEntity.fromDomain(domain)
        assertEquals(id, roundTripped.id)
        assertEquals("Arroz", roundTripped.name)
        assertEquals(expiryDate, roundTripped.expiryDate)
        assertEquals("Comprar integral", roundTripped.notes)
    }

    @Test
    fun `toDomain and fromDomain round-trip with nullable fields null`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 3, 1, 10, 0)
        val updatedAt = LocalDateTime.of(2026, 3, 1, 10, 0)
        val entity = InventoryItemJpaEntity(
            id = id,
            houseId = houseId,
            name = "Sabão",
            category = Category.CLEANING,
            quantityLevel = QuantityLevel.RUNNING_OUT,
            expiryDate = null,
            notes = null,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        val domain = entity.toDomain()

        assertNull(domain.expiryDate)
        assertNull(domain.notes)
        assertEquals("Sabão", domain.name)

        val roundTripped = InventoryItemJpaEntity.fromDomain(domain)
        assertNull(roundTripped.expiryDate)
        assertNull(roundTripped.notes)
    }

    @Test
    fun `fromDomain preserves houseId, category, quantityLevel, createdAt and updatedAt`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 4, 1, 8, 0)
        val updatedAt = LocalDateTime.of(2026, 4, 1, 9, 0)
        val item = InventoryItem(
            id = id, houseId = houseId, name = "Leite",
            category = Category.FOOD, quantityLevel = QuantityLevel.ENOUGH,
            expiryDate = null, notes = null, createdAt = createdAt, updatedAt = updatedAt
        )

        val entity = InventoryItemJpaEntity.fromDomain(item)

        assertEquals(houseId, entity.houseId)
        assertEquals(Category.FOOD, entity.category)
        assertEquals(QuantityLevel.ENOUGH, entity.quantityLevel)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }

    @Test
    fun `constructor defaults expiryDate and notes to null when not specified`() {
        val entity = InventoryItemJpaEntity(
            id = UUID.randomUUID(),
            houseId = UUID.randomUUID(),
            name = "Feijão",
            category = Category.FOOD,
            quantityLevel = QuantityLevel.RUNNING_OUT,
            createdAt = LocalDateTime.of(2026, 5, 1, 0, 0),
            updatedAt = LocalDateTime.of(2026, 5, 1, 0, 0)
        )

        assertNull(entity.expiryDate)
        assertNull(entity.notes)
    }
}
