package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository
) : UpdateItemQuantityUseCase {

    override fun updateQuantity(command: UpdateItemQuantityCommand): InventoryItem {
        val item = inventoryRepository.findById(command.itemId)
            ?: throw NoSuchElementException("Item not found: ${command.itemId}")
        val updated = item.copy(
            quantityLevel = command.quantityLevel,
            updatedAt = LocalDateTime.now()
        )
        return inventoryRepository.save(updated)
    }
}
