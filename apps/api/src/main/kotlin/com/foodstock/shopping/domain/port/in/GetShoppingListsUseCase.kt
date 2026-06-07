package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

interface GetShoppingListsUseCase {
    fun getShoppingLists(houseId: UUID): List<ShoppingList>
}
