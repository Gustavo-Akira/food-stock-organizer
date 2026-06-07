package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

interface GetShoppingListUseCase {
    fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>>
}
