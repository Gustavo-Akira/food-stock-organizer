package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingItemNotFoundException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import jakarta.persistence.OptimisticLockException
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort,
    private val memberRolePort: MemberRolePort,
    private val restockItemsPort: RestockItemsPort,
    private val clock: Clock
) : GenerateShoppingListUseCase, GetShoppingListsUseCase, GetShoppingListUseCase,
    StartShoppingUseCase, CompleteShoppingUseCase, CancelShoppingUseCase,
    AddShoppingItemUseCase, RemoveShoppingItemUseCase, UpdateShoppingItemUseCase {

    override fun generateFromRunningOutItems(command: GenerateShoppingListCommand): ShoppingList {
        val now = LocalDateTime.now(clock)
        val list = ShoppingList(
            id = UUID.randomUUID(),
            houseId = command.houseId,
            name = command.listName,
            status = ShoppingListStatus.OPEN,
            createdBy = command.createdBy,
            createdAt = now,
            updatedAt = now
        )
        val savedList = shoppingListRepository.save(list)

        runningOutItemsQueryPort.findRunningOutItems(command.houseId).forEach { item ->
            shoppingListRepository.saveItem(
                ShoppingListItem(UUID.randomUUID(), savedList.id, item.itemId, item.name, 1, false, now)
            )
        }

        return savedList
    }

    override fun getShoppingLists(houseId: UUID): List<ShoppingList> =
        shoppingListRepository.findAllByHouseId(houseId)

    override fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>> {
        val list = shoppingListRepository.findById(listId) ?: throw ShoppingListNotFoundException(listId)
        return Pair(list, shoppingListRepository.findItemsByListId(listId))
    }

    override fun start(command: StartShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status != ShoppingListStatus.OPEN)
            throw InvalidShoppingListStateException("Cannot start shopping: list is ${list.status}")
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.SHOPPING, updatedAt = LocalDateTime.now(clock)))
    }

    @Transactional
    override fun complete(command: CompleteShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status != ShoppingListStatus.SHOPPING)
            throw InvalidShoppingListStateException("Cannot complete: list is ${list.status}")
        val restockIds = shoppingListRepository.findItemsByListId(command.listId)
            .filter { it.checked && it.inventoryItemId != null }
            .mapNotNull { it.inventoryItemId }
        if (restockIds.isNotEmpty()) restockItemsPort.restock(restockIds)
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.COMPLETED, updatedAt = LocalDateTime.now(clock)))
    }

    override fun cancel(command: CancelShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status !in listOf(ShoppingListStatus.OPEN, ShoppingListStatus.SHOPPING))
            throw InvalidShoppingListStateException("Cannot cancel: list is ${list.status}")
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.CANCELLED, updatedAt = LocalDateTime.now(clock)))
    }

    override fun addItem(command: AddShoppingItemCommand): ShoppingListItem {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        if (command.inventoryItemId != null) {
            val duplicate = shoppingListRepository.findItemsByListId(command.listId)
                .any { it.inventoryItemId == command.inventoryItemId }
            if (duplicate) throw InvalidShoppingListStateException("Item with inventoryItemId ${command.inventoryItemId} already exists in list")
        }
        val item = ShoppingListItem(UUID.randomUUID(), command.listId, command.inventoryItemId,
            command.name, command.quantity, false, LocalDateTime.now(clock))
        val saved = shoppingListRepository.saveItem(item)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
        return saved
    }

    override fun removeItem(command: RemoveShoppingItemCommand) {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        val item = shoppingListRepository.findItemById(command.itemId)
        if (item == null || item.shoppingListId != command.listId) throw ShoppingItemNotFoundException(command.itemId)
        shoppingListRepository.deleteItem(command.itemId)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
    }

    override fun updateItem(command: UpdateShoppingItemCommand): ShoppingListItem {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        val item = shoppingListRepository.findItemById(command.itemId)
        if (item == null || item.shoppingListId != command.listId) throw ShoppingItemNotFoundException(command.itemId)
        val updated = item.copy(
            quantity = command.quantity ?: item.quantity,
            checked = command.checked ?: item.checked
        )
        val saved = shoppingListRepository.updateItem(updated)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
        return saved
    }

    private fun assertVersion(current: Long, provided: Long) {
        if (current != provided) throw OptimisticLockException("Shopping list version mismatch")
    }

    private fun requireOwner(houseId: UUID, userId: UUID) {
        if (memberRolePort.getRole(houseId, userId) != HouseRole.OWNER)
            throw ShoppingListAccessDeniedException("Only the house owner can perform this action")
    }

    private fun requireActiveMember(houseId: UUID, userId: UUID) {
        memberRolePort.getRole(houseId, userId)
            ?: throw ShoppingListAccessDeniedException("User is not an active member of this house")
    }

    private fun assertMutable(list: ShoppingList) {
        if (list.status !in listOf(ShoppingListStatus.OPEN, ShoppingListStatus.SHOPPING))
            throw InvalidShoppingListStateException("Cannot modify items on a ${list.status} list")
    }
}
