package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import java.util.UUID

interface GetInventoryItemUseCase {
    fun getInventoryItem(itemId: UUID): InventoryItem
}
