package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

interface GetInventoryUseCase {
    fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?): List<InventoryItem>
}
