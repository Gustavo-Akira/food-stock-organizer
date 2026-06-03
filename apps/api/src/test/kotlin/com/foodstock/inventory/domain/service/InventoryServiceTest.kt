package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    private val inventoryRepository: InventoryRepository = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = InventoryService(inventoryRepository, fixedClock)

    @Test
    fun `addItem saves item with generated id and timestamps`() {
        val houseId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val command = AddItemCommand(
            houseId = houseId, name = "Arroz", category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY, expiryDate = LocalDate.of(2026, 12, 31), notes = null
        )
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(houseId, result.houseId)
        assertEquals("Arroz", result.name)
        assertEquals(Category.FOOD, result.category)
        assertEquals(QuantityLevel.PLENTY, result.quantityLevel)
        assertEquals(LocalDate.of(2026, 12, 31), result.expiryDate)
        assertNotNull(result.id)
        assertEquals(expectedNow, result.createdAt)
        assertEquals(expectedNow, result.updatedAt)
    }

    @Test
    fun `addItem saves item without optional fields`() {
        val command = AddItemCommand(
            houseId = UUID.randomUUID(), name = "Sabão", category = Category.CLEANING,
            quantityLevel = QuantityLevel.RUNNING_OUT, expiryDate = null, notes = null
        )
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(command.houseId, result.houseId)
        assertEquals("Sabão", result.name)
        assertEquals(Category.CLEANING, result.category)
        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
        assertEquals(null, result.expiryDate)
        assertEquals(null, result.notes)
        assertNotNull(result.id)
    }

    @Test
    fun `updateQuantity updates quantityLevel on existing item`() {
        val itemId = UUID.randomUUID()
        val pastInstant = Instant.parse("2025-01-01T00:00:00Z")
        val existing = InventoryItem(
            id = itemId, houseId = UUID.randomUUID(), name = "Leite",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.ofInstant(pastInstant, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(pastInstant, ZoneOffset.UTC)
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(existing)
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.RUNNING_OUT)
        )

        val expectedUpdatedAt = LocalDateTime.now(fixedClock)
        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
        assertEquals(itemId, result.id)

        val captor = argumentCaptor<InventoryItem>()
        verify(inventoryRepository).save(captor.capture())
        assertEquals(QuantityLevel.RUNNING_OUT, captor.firstValue.quantityLevel)
        assertEquals(expectedUpdatedAt, captor.firstValue.updatedAt)
    }

    @Test
    fun `updateQuantity throws NoSuchElementException when item not found`() {
        val itemId = UUID.randomUUID()
        whenever(inventoryRepository.findById(itemId)).thenReturn(null)

        assertThrows<NoSuchElementException> {
            service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.ENOUGH))
        }
    }
}
