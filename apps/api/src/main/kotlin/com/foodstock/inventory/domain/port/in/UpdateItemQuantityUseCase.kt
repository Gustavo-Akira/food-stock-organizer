package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

data class UpdateItemQuantityCommand(
    val itemId: UUID,
    val quantityLevel: QuantityLevel
)

interface UpdateItemQuantityUseCase {
    fun updateQuantity(command: UpdateItemQuantityCommand): InventoryItem
}
