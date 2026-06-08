package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListItemSummary(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val inventoryItemId: UUID?,
    val createdAt: LocalDateTime
)

data class ShoppingListDetailResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val items: List<ShoppingListItemSummary>
)

fun Pair<ShoppingList, List<ShoppingListItem>>.toDetailResponse(): ShoppingListDetailResponse {
    val (list, items) = this
    return ShoppingListDetailResponse(
        id = list.id,
        houseId = list.houseId,
        name = list.name,
        status = list.status,
        createdBy = list.createdBy,
        createdAt = list.createdAt,
        updatedAt = list.updatedAt,
        items = items.map { item ->
            ShoppingListItemSummary(
                id = item.id,
                name = item.name,
                quantity = item.quantity,
                checked = item.checked,
                inventoryItemId = item.inventoryItemId,
                createdAt = item.createdAt
            )
        }
    )
}
