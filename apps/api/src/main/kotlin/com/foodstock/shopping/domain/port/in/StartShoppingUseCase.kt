package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class StartShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface StartShoppingUseCase {
    fun start(command: StartShoppingCommand): ShoppingList
}
