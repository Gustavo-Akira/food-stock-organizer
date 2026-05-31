package com.foodstock.shopping.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListItem(
    val id: UUID,
    val shoppingListId: UUID,
    val inventoryItemId: UUID?,
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val createdAt: LocalDateTime
)
