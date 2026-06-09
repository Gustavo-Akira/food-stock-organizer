package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class CancelShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface CancelShoppingUseCase {
    fun cancel(command: CancelShoppingCommand): ShoppingList
}
