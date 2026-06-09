package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingItemNotFoundException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import com.foodstock.shopping.domain.port.out.RunningOutItem
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import jakarta.persistence.OptimisticLockException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
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
    private val memberRolePort: MemberRolePort = mock()
    private val restockItemsPort: RestockItemsPort = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = ShoppingListService(
        shoppingListRepository, runningOutItemsQueryPort, memberRolePort, restockItemsPort, fixedClock
    )

    // --- Existing tests ---

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
        verify(shoppingListRepository, times(2)).saveItem(any())

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

    @Test
    fun `getShoppingLists returns lists for house`() {
        val houseId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val list = ShoppingList(
            id = UUID.randomUUID(), houseId = houseId, name = "Weekly",
            status = ShoppingListStatus.OPEN, createdBy = UUID.randomUUID(),
            createdAt = now, updatedAt = now
        )
        whenever(shoppingListRepository.findAllByHouseId(houseId)).thenReturn(listOf(list))

        val result = service.getShoppingLists(houseId)

        assertEquals(1, result.size)
        assertEquals(houseId, result[0].houseId)
        assertEquals("Weekly", result[0].name)
    }

    @Test
    fun `getShoppingLists returns empty list when house has no lists`() {
        val houseId = UUID.randomUUID()
        whenever(shoppingListRepository.findAllByHouseId(houseId)).thenReturn(emptyList())

        val result = service.getShoppingLists(houseId)

        assertEquals(0, result.size)
    }

    @Test
    fun `getShoppingList returns list with its items`() {
        val listId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val list = ShoppingList(
            id = listId, houseId = houseId, name = "Weekly",
            status = ShoppingListStatus.OPEN, createdBy = UUID.randomUUID(),
            createdAt = now, updatedAt = now
        )
        val item = ShoppingListItem(
            id = UUID.randomUUID(), shoppingListId = listId, inventoryItemId = null,
            name = "Milk", quantity = 1, checked = false, createdAt = now
        )
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(item))

        val (resultList, resultItems) = service.getShoppingList(listId)

        assertEquals(listId, resultList.id)
        assertEquals(1, resultItems.size)
        assertEquals("Milk", resultItems[0].name)
    }

    @Test
    fun `getShoppingList throws ShoppingListNotFoundException when list does not exist`() {
        val listId = UUID.randomUUID()
        whenever(shoppingListRepository.findById(listId)).thenReturn(null)

        assertThrows<ShoppingListNotFoundException> { service.getShoppingList(listId) }
    }

    // --- State transitions ---

    private fun aList(
        listId: UUID = UUID.randomUUID(),
        houseId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        status: ShoppingListStatus = ShoppingListStatus.OPEN,
        version: Long = 0
    ): ShoppingList {
        val now = LocalDateTime.now(fixedClock)
        return ShoppingList(id = listId, houseId = houseId, name = "Weekly", status = status,
            createdBy = userId, createdAt = now, updatedAt = now, version = version)
    }

    @Test
    fun `start transitions OPEN list to SHOPPING`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.start(StartShoppingCommand(listId, userId, listVersion = 0))

        assertEquals(ShoppingListStatus.SHOPPING, result.status)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `start throws InvalidShoppingListStateException when list is not OPEN`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `start throws ShoppingListAccessDeniedException when caller is not OWNER`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)

        assertThrows<ShoppingListAccessDeniedException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `start throws OptimisticLockException when version is stale`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, version = 2)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)

        assertThrows<OptimisticLockException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 1))
        }
    }

    @Test
    fun `complete restocks checked inventory items and marks list COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val inventoryItemId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 1)
        val now = LocalDateTime.now(fixedClock)
        val checkedItem = ShoppingListItem(UUID.randomUUID(), listId, inventoryItemId, "Milk", 1, true, now)
        val uncheckedItem = ShoppingListItem(UUID.randomUUID(), listId, null, "Manual", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(checkedItem, uncheckedItem))
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.complete(CompleteShoppingCommand(listId, userId, listVersion = 1))

        assertEquals(ShoppingListStatus.COMPLETED, result.status)
        verify(restockItemsPort).restock(listOf(inventoryItemId))
    }

    @Test
    fun `complete does not call restock when no checked inventory items exist`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(emptyList())
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        service.complete(CompleteShoppingCommand(listId, userId, listVersion = 0))

        verify(restockItemsPort, never()).restock(any())
    }

    @Test
    fun `complete throws InvalidShoppingListStateException when list is not SHOPPING`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.complete(CompleteShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `cancel transitions OPEN list to CANCELLED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.cancel(CancelShoppingCommand(listId, userId, listVersion = 0))

        assertEquals(ShoppingListStatus.CANCELLED, result.status)
    }

    @Test
    fun `cancel throws InvalidShoppingListStateException when list is already COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.COMPLETED, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.cancel(CancelShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    // --- Item mutations ---

    @Test
    fun `addItem saves new item and bumps list updatedAt`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(emptyList())
        whenever(shoppingListRepository.saveItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 2))

        assertEquals("Bread", result.name)
        assertEquals(2, result.quantity)
        assertEquals(false, result.checked)
        assertEquals(listId, result.shoppingListId)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `addItem throws InvalidShoppingListStateException when list is COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.COMPLETED, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)

        assertThrows<InvalidShoppingListStateException> {
            service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 1))
        }
    }

    @Test
    fun `addItem throws InvalidShoppingListStateException when inventoryItemId already in list`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val inventoryItemId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val existing = ShoppingListItem(UUID.randomUUID(), listId, inventoryItemId, "Milk", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(existing))

        assertThrows<InvalidShoppingListStateException> {
            service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Milk", quantity = 1, inventoryItemId = inventoryItemId))
        }
    }

    @Test
    fun `addItem allows duplicate names when inventoryItemId is null`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val existing = ShoppingListItem(UUID.randomUUID(), listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(existing))
        whenever(shoppingListRepository.saveItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 1))

        assertEquals("Bread", result.name)
    }

    @Test
    fun `removeItem deletes item and bumps list updatedAt`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        service.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion = 0))

        verify(shoppingListRepository).deleteItem(itemId)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `removeItem throws ShoppingItemNotFoundException when item belongs to a different list`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, UUID.randomUUID(), null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)

        assertThrows<ShoppingItemNotFoundException> {
            service.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion = 0))
        }
    }

    @Test
    fun `updateItem applies quantity and checked changes`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 1)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.updateItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.updateItem(UpdateShoppingItemCommand(listId, itemId, userId, listVersion = 1, quantity = 3, checked = true))

        assertEquals(3, result.quantity)
        assertEquals(true, result.checked)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `updateItem preserves existing values for null fields`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 5, true, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.updateItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.updateItem(UpdateShoppingItemCommand(listId, itemId, userId, listVersion = 0, quantity = null, checked = false))

        assertEquals(5, result.quantity)
        assertEquals(false, result.checked)
    }
}
