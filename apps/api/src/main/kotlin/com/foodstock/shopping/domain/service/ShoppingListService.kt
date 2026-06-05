package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort,
    private val clock: Clock
) : GenerateShoppingListUseCase {

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

        val runningOutItems = runningOutItemsQueryPort.findRunningOutItems(command.houseId)
        runningOutItems.forEach { item ->
            shoppingListRepository.saveItem(
                ShoppingListItem(
                    id = UUID.randomUUID(),
                    shoppingListId = savedList.id,
                    inventoryItemId = item.itemId,
                    name = item.name,
                    quantity = 1,
                    checked = false,
                    createdAt = now
                )
            )
        }

        return savedList
    }
}
