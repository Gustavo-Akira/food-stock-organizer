package com.foodstock.shopping.adapter.`in`.dto

import jakarta.validation.constraints.Min

data class UpdateItemRequest(
    @field:Min(1) val quantity: Int? = null,
    val checked: Boolean? = null
)
