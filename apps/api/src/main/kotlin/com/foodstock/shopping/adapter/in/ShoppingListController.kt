package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.adapter.`in`.dto.AddItemRequest
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListDetailResponse
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListItemResponse
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListResponse
import com.foodstock.shopping.adapter.`in`.dto.UpdateItemRequest
import com.foodstock.shopping.adapter.`in`.dto.toDetailResponse
import com.foodstock.shopping.adapter.`in`.dto.toItemResponse
import com.foodstock.shopping.adapter.`in`.dto.toResponse
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/shopping-lists")
class ShoppingListController(
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val getShoppingListsUseCase: GetShoppingListsUseCase,
    private val getShoppingListUseCase: GetShoppingListUseCase,
    private val startShoppingUseCase: StartShoppingUseCase,
    private val completeShoppingUseCase: CompleteShoppingUseCase,
    private val cancelShoppingUseCase: CancelShoppingUseCase,
    private val addShoppingItemUseCase: AddShoppingItemUseCase,
    private val removeShoppingItemUseCase: RemoveShoppingItemUseCase,
    private val updateShoppingItemUseCase: UpdateShoppingItemUseCase
) {

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateList(
        @RequestBody request: GenerateShoppingListRequest,
        @RequestHeader("X-User-Id") userId: UUID
    ): ShoppingListResponse =
        generateShoppingListUseCase.generateFromRunningOutItems(
            GenerateShoppingListCommand(request.houseId, userId, request.listName)
        ).toResponse()

    @GetMapping
    fun getShoppingLists(@RequestHeader("X-House-Id") houseId: UUID): List<ShoppingListResponse> =
        getShoppingListsUseCase.getShoppingLists(houseId).map { it.toResponse() }

    @GetMapping("/{listId}")
    fun getShoppingList(@PathVariable listId: UUID): ShoppingListDetailResponse =
        getShoppingListUseCase.getShoppingList(listId).toDetailResponse()

    @PostMapping("/{listId}/start")
    fun startShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        startShoppingUseCase.start(StartShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/complete")
    fun completeShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        completeShoppingUseCase.complete(CompleteShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/cancel")
    fun cancelShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        cancelShoppingUseCase.cancel(CancelShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long,
        @RequestBody @Valid request: AddItemRequest
    ): ShoppingListItemResponse =
        addShoppingItemUseCase.addItem(
            AddShoppingItemCommand(listId, userId, listVersion, request.name, request.quantity, request.inventoryItemId)
        ).toItemResponse()

    @DeleteMapping("/{listId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(
        @PathVariable listId: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ) = removeShoppingItemUseCase.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion))

    @PatchMapping("/{listId}/items/{itemId}")
    fun updateItem(
        @PathVariable listId: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long,
        @RequestBody request: UpdateItemRequest
    ): ShoppingListItemResponse {
        if (request.quantity == null && request.checked == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one of quantity or checked must be provided")
        return updateShoppingItemUseCase.updateItem(
            UpdateShoppingItemCommand(listId, itemId, userId, listVersion, request.quantity, request.checked)
        ).toItemResponse()
    }
}
