package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.QuantityLevel
import jakarta.validation.constraints.NotNull

data class UpdateQuantityRequest(
    @field:NotNull
    val quantityLevel: QuantityLevel
)
