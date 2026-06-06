package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InventoryItemResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun InventoryItem.toResponse() = InventoryItemResponse(
    id = id,
    houseId = houseId,
    name = name,
    category = category,
    quantityLevel = quantityLevel,
    expiryDate = expiryDate,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
