package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

data class UpdateShoppingItemCommand(
    val listId: UUID,
    val itemId: UUID,
    val userId: UUID,
    val listVersion: Long,
    val quantity: Int? = null,
    val checked: Boolean? = null
)

interface UpdateShoppingItemUseCase {
    fun updateItem(command: UpdateShoppingItemCommand): ShoppingListItem
}
