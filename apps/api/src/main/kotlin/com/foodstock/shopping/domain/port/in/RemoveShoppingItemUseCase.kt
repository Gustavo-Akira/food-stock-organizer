package com.foodstock.shopping.domain.port.`in`

import java.util.UUID

data class RemoveShoppingItemCommand(
    val listId: UUID,
    val itemId: UUID,
    val userId: UUID,
    val listVersion: Long
)

interface RemoveShoppingItemUseCase {
    fun removeItem(command: RemoveShoppingItemCommand)
}
