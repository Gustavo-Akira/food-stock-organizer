package com.foodstock.inventory.adapter.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class UpdateQuantityRequest(val quantityLevel: QuantityLevel)

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val updateItemQuantityUseCase: UpdateItemQuantityUseCase
) {

    @PatchMapping("/{itemId}/quantity")
    fun updateQuantity(
        @PathVariable itemId: UUID,
        @RequestBody request: UpdateQuantityRequest
    ): InventoryItem {
        return updateItemQuantityUseCase.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = request.quantityLevel)
        )
    }
}
