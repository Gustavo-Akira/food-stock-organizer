package com.foodstock.inventory.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(InventoryController::class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var addItemUseCase: AddItemUseCase

    @MockBean
    private lateinit var updateItemQuantityUseCase: UpdateItemQuantityUseCase

    @Test
    fun `addItem returns created inventory item response`() {
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(addItemUseCase.addItem(any())).thenReturn(item)

        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", item.houseId.toString())
            content = objectMapper.writeValueAsString(
                AddItemRequest(
                    name = "Arroz",
                    category = Category.FOOD,
                    quantityLevel = QuantityLevel.PLENTY,
                    expiryDate = LocalDate.parse("2026-12-31"),
                    notes = "Pacote fechado"
                )
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(item.id.toString()) }
                jsonPath("$.houseId") { value(item.houseId.toString()) }
                jsonPath("$.name") { value("Arroz") }
                jsonPath("$.category") { value("FOOD") }
                jsonPath("$.quantityLevel") { value("PLENTY") }
                jsonPath("$.expiryDate") { value("2026-12-31") }
                jsonPath("$.notes") { value("Pacote fechado") }
            }
    }

    @Test
    fun `updateQuantity returns updated inventory item response`() {
        val itemId = UUID.fromString("99999999-9999-9999-9999-999999999999")
        val item = inventoryItem(id = itemId, quantityLevel = QuantityLevel.RUNNING_OUT)
        whenever(updateItemQuantityUseCase.updateQuantity(any())).thenReturn(item)

        mockMvc.patch("/api/v1/inventory/$itemId/quantity") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateQuantityRequest(QuantityLevel.RUNNING_OUT))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(itemId.toString()) }
                jsonPath("$.quantityLevel") { value("RUNNING_OUT") }
            }
    }

    @Test
    fun `addItem rejects blank name`() {
        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "",
                    "category" to "FOOD",
                    "quantityLevel" to "PLENTY",
                    "expiryDate" to null,
                    "notes" to null
                )
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `addItem rejects null quantity level`() {
        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Arroz",
                    "category" to "FOOD",
                    "quantityLevel" to null,
                    "expiryDate" to null,
                    "notes" to null
                )
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    private fun inventoryItem(
        id: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        houseId: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        quantityLevel: QuantityLevel
    ) = InventoryItem(
        id = id,
        houseId = houseId,
        name = "Arroz",
        category = Category.FOOD,
        quantityLevel = quantityLevel,
        expiryDate = LocalDate.parse("2026-12-31"),
        notes = "Pacote fechado",
        createdAt = LocalDateTime.parse("2026-06-05T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-06-05T12:30:00")
    )
}
