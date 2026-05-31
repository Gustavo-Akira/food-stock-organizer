package com.foodstock.shopping.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class ShoppingListStatus { OPEN, SHOPPING, COMPLETED }

data class ShoppingList(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
