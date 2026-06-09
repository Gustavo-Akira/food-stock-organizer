package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListItemResponse(
    val id: UUID,
    val shoppingListId: UUID,
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val inventoryItemId: UUID?,
    val createdAt: LocalDateTime
)

fun ShoppingListItem.toItemResponse() = ShoppingListItemResponse(
    id = id, shoppingListId = shoppingListId, name = name, quantity = quantity,
    checked = checked, inventoryItemId = inventoryItemId, createdAt = createdAt
)
