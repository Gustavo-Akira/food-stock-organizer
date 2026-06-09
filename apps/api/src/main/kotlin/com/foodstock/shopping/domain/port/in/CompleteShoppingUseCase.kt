package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class CompleteShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface CompleteShoppingUseCase {
    fun complete(command: CompleteShoppingCommand): ShoppingList
}
