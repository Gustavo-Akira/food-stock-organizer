package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun ShoppingList.toResponse() = ShoppingListResponse(
    id = id,
    houseId = houseId,
    name = name,
    status = status,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)
