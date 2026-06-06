package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListResponse
import com.foodstock.shopping.adapter.`in`.dto.toResponse
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

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
    ): ShoppingListResponse {
        return generateShoppingListUseCase.generateFromRunningOutItems(
            GenerateShoppingListCommand(
                houseId = request.houseId,
                createdBy = userId,
                listName = request.listName
            )
        ).toResponse()
    }
}
