package com.foodstock.shopping.domain.port.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

interface ShoppingListRepository {
    fun save(list: ShoppingList): ShoppingList
    fun update(list: ShoppingList): ShoppingList
    fun saveItem(item: ShoppingListItem): ShoppingListItem
    fun updateItem(item: ShoppingListItem): ShoppingListItem
    fun deleteItem(itemId: UUID)
    fun findById(id: UUID): ShoppingList?
    fun findItemById(itemId: UUID): ShoppingListItem?
    fun findAllByHouseId(houseId: UUID): List<ShoppingList>
    fun findItemsByListId(listId: UUID): List<ShoppingListItem>
}
