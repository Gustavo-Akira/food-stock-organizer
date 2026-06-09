package com.foodstock.inventory.domain.port.out

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

interface InventoryRepository {
    fun save(item: InventoryItem): InventoryItem
    fun findById(id: UUID): InventoryItem?
    fun findAllByHouseId(houseId: UUID): List<InventoryItem>
    fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItem>
    fun deleteById(id: UUID)
    fun updateQuantityLevel(id: UUID, level: QuantityLevel)
}
