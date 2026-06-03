package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.time.LocalDate
import java.util.UUID

data class AddItemCommand(
    val houseId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?
)

interface AddItemUseCase {
    fun addItem(command: AddItemCommand): InventoryItem
}
