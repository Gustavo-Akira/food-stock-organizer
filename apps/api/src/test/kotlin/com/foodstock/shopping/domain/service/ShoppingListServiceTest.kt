package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.out.RunningOutItem
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ShoppingListServiceTest {

    private val shoppingListRepository: ShoppingListRepository = mock()
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = ShoppingListService(shoppingListRepository, runningOutItemsQueryPort, fixedClock)

    @Test
    fun `generateFromRunningOutItems saves list and one item per running-out item`() {
        val houseId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        val command = GenerateShoppingListCommand(houseId = houseId, listName = "Weekly Shop", createdBy = createdBy)
        val expectedNow = LocalDateTime.now(fixedClock)

        val runningOutItems = listOf(
            RunningOutItem(itemId = UUID.randomUUID(), name = "Arroz"),
            RunningOutItem(itemId = UUID.randomUUID(), name = "Feijão")
        )
        whenever(runningOutItemsQueryPort.findRunningOutItems(houseId)).thenReturn(runningOutItems)
        whenever(shoppingListRepository.save(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.generateFromRunningOutItems(command)

        verify(shoppingListRepository).save(any())
        verify(shoppingListRepository, org.mockito.kotlin.times(2)).saveItem(any())

        assertEquals(houseId, result.houseId)
        assertEquals("Weekly Shop", result.name)
        assertEquals(ShoppingListStatus.OPEN, result.status)
        assertEquals(createdBy, result.createdBy)
        assertEquals(expectedNow, result.createdAt)
        assertEquals(expectedNow, result.updatedAt)
        assertNotNull(result.id)
    }

    @Test
    fun `generateFromRunningOutItems with empty running-out list saves list and no items`() {
        val houseId = UUID.randomUUID()
        val createdBy = UUID.randomUUID()
        val command = GenerateShoppingListCommand(houseId = houseId, listName = "Empty Shop", createdBy = createdBy)

        whenever(runningOutItemsQueryPort.findRunningOutItems(houseId)).thenReturn(emptyList())
        whenever(shoppingListRepository.save(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.generateFromRunningOutItems(command)

        verify(shoppingListRepository).save(any())
        verify(shoppingListRepository, never()).saveItem(any())

        assertEquals(houseId, result.houseId)
        assertEquals("Empty Shop", result.name)
        assertNotNull(result.id)
    }
}
