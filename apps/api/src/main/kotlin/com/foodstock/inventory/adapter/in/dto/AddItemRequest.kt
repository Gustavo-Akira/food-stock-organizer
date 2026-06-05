package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.QuantityLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AddItemRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotNull
    val category: Category,
    @field:NotNull
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    @field:Size(max = 1000)
    val notes: String?
)
