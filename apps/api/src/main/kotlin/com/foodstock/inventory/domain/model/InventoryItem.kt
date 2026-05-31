package com.foodstock.inventory.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class QuantityLevel { RUNNING_OUT, ENOUGH, PLENTY }

enum class Category { FOOD, CLEANING, HYGIENE, OTHER }

data class InventoryItem(
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
