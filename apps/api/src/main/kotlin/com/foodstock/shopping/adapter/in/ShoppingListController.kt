package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class GenerateShoppingListRequest(
    val houseId: UUID,
    val listName: String = "Lista de Compras"
)

@RestController
@RequestMapping("/api/v1/shopping-lists")
class ShoppingListController(
    private val generateShoppingListUseCase: GenerateShoppingListUseCase
) {

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateList(
        @RequestBody request: GenerateShoppingListRequest,
        @RequestHeader("X-User-Id") userId: UUID
    ): ShoppingList {
        return generateShoppingListUseCase.generateFromRunningOutItems(
            GenerateShoppingListCommand(
                houseId = request.houseId,
                createdBy = userId,
                listName = request.listName
            )
        )
    }
}
