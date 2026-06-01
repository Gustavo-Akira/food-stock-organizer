package com.foodstock.inventory.adapter.`in`

import com.foodstock.inventory.domain.model.Category
// imported only to support the private toResponse() extension — never returned as HTTP response
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AddItemRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    @field:Size(max = 1000)
    val notes: String?
)

data class UpdateQuantityRequest(val quantityLevel: QuantityLevel)

data class InventoryItemResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val addItemUseCase: AddItemUseCase,
    private val updateItemQuantityUseCase: UpdateItemQuantityUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @Valid @RequestBody request: AddItemRequest,
        // TODO: replace with @AuthenticationPrincipal once JWT filter is wired
        @RequestHeader("X-House-Id") houseId: UUID
    ): InventoryItemResponse {
        val item = addItemUseCase.addItem(
            AddItemCommand(
                houseId = houseId,
                name = request.name,
                category = request.category,
                quantityLevel = request.quantityLevel,
                expiryDate = request.expiryDate,
                notes = request.notes
            )
        )
        return item.toResponse()
    }

    @PatchMapping("/{itemId}/quantity")
    @ResponseStatus(HttpStatus.OK)
    fun updateQuantity(
        @PathVariable itemId: UUID,
        @RequestBody request: UpdateQuantityRequest
    ): InventoryItemResponse {
        return updateItemQuantityUseCase.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = request.quantityLevel)
        ).toResponse()
    }
}

private fun InventoryItem.toResponse() = InventoryItemResponse(
    id = id, houseId = houseId, name = name, category = category,
    quantityLevel = quantityLevel, expiryDate = expiryDate, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt
)
