package com.foodstock.shopping.adapter.out

import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InventoryRestockAdapter(
    private val inventoryRepository: InventoryRepository
) : RestockItemsPort {

    override fun restock(itemIds: List<UUID>) {
        itemIds.forEach { inventoryRepository.updateQuantityLevel(it, QuantityLevel.ENOUGH) }
    }
}
