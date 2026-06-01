package com.foodstock.shopping.adapter.out

import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import com.foodstock.shopping.domain.port.out.RunningOutItem
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Adapter that bridges the inventory domain into the shopping domain
 * via the RunningOutItemsQueryPort anti-corruption interface.
 * The shopping domain service never depends on inventory internals directly.
 */
@Component
class InventoryRunningOutAdapter(
    private val inventoryRepository: InventoryRepository
) : RunningOutItemsQueryPort {

    override fun findRunningOutItems(houseId: UUID): List<RunningOutItem> =
        inventoryRepository
            .findAllByHouseIdAndQuantityLevel(houseId, QuantityLevel.RUNNING_OUT)
            .map { RunningOutItem(itemId = it.id, name = it.name) }
}
