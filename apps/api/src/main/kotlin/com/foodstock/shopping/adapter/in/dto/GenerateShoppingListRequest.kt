package com.foodstock.shopping.adapter.`in`.dto

import java.util.UUID

data class GenerateShoppingListRequest(
    val houseId: UUID,
    val listName: String = "Lista de Compras"
)
