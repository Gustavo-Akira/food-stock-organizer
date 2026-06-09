package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

data class AddShoppingItemCommand(
    val listId: UUID,
    val userId: UUID,
    val listVersion: Long,
    val name: String,
    val quantity: Int,
    val inventoryItemId: UUID? = null
)

interface AddShoppingItemUseCase {
    fun addItem(command: AddShoppingItemCommand): ShoppingListItem
}
