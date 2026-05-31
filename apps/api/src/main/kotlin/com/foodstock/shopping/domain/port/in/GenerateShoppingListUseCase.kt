package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class GenerateShoppingListCommand(
    val houseId: UUID,
    val createdBy: UUID,
    val listName: String = "Lista de Compras"
)

interface GenerateShoppingListUseCase {
    /**
     * Generates a shopping list pre-populated with inventory items
     * that have QuantityLevel.RUNNING_OUT.
     */
    fun generateFromRunningOutItems(command: GenerateShoppingListCommand): ShoppingList
}
