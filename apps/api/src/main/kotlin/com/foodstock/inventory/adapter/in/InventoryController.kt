package com.foodstock.inventory.adapter.`in`

import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.InventoryItemResponse
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.adapter.`in`.dto.toResponse
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val addItemUseCase: AddItemUseCase,
    private val updateItemQuantityUseCase: UpdateItemQuantityUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getInventoryItemUseCase: GetInventoryItemUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @Valid @RequestBody request: AddItemRequest,
        // TODO: replace with houseId extracted from JWT claims once @AuthenticationPrincipal is wired;
        //  authentication is enforced (SecurityConfig.anyRequest().authenticated()) but ownership is not yet verified
        @RequestHeader("X-House-Id") houseId: UUID
    ): InventoryItemResponse {
        val item = addItemUseCase.addItem(
            AddItemCommand(
                houseId = houseId,
                name = request.name.trim(),
                category = request.category,
                quantityLevel = request.quantityLevel,
                expiryDate = request.expiryDate,
                notes = request.notes
            )
        )
        return item.toResponse()
    }

    @PatchMapping("/{itemId}/quantity")
    fun updateQuantity(
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: UpdateQuantityRequest
    ): InventoryItemResponse {
        return updateItemQuantityUseCase.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = request.quantityLevel)
        ).toResponse()
    }

    @GetMapping
    fun getInventory(
        @RequestHeader("X-House-Id") houseId: UUID,
        @RequestParam(required = false) quantityLevel: QuantityLevel?
    ): List<InventoryItemResponse> =
        getInventoryUseCase.getInventory(houseId, quantityLevel).map { it.toResponse() }

    @GetMapping("/{itemId}")
    fun getInventoryItem(@PathVariable itemId: UUID): InventoryItemResponse =
        getInventoryItemUseCase.getInventoryItem(itemId).toResponse()
}
