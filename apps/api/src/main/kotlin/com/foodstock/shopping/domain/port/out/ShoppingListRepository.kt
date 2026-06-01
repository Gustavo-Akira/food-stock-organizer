package com.foodstock.shopping.domain.port.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

interface ShoppingListRepository {
    fun save(list: ShoppingList): ShoppingList
    fun saveItem(item: ShoppingListItem): ShoppingListItem
    fun findById(id: UUID): ShoppingList?
    fun findAllByHouseId(houseId: UUID): List<ShoppingList>
    fun findItemsByListId(listId: UUID): List<ShoppingListItem>
}
