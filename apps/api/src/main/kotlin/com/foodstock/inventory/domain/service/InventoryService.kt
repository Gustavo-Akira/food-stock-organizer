package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import com.foodstock.inventory.domain.port.out.InventoryRepository
import java.time.LocalDateTime
import java.util.UUID

class InventoryService(
    private val inventoryRepository: InventoryRepository
) : AddItemUseCase, UpdateItemQuantityUseCase {

    override fun addItem(command: AddItemCommand): InventoryItem {
        val now = LocalDateTime.now()
        val item = InventoryItem(
            id = UUID.randomUUID(),
            houseId = command.houseId,
            name = command.name,
            category = command.category,
            quantityLevel = command.quantityLevel,
            expiryDate = command.expiryDate,
            notes = command.notes,
            createdAt = now,
            updatedAt = now
        )
        return inventoryRepository.save(item)
    }

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
