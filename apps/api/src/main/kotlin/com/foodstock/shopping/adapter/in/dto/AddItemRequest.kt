package com.foodstock.shopping.adapter.`in`.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddItemRequest(
    @field:NotBlank val name: String,
    @field:Min(1) val quantity: Int,
    val inventoryItemId: UUID? = null
)
