package com.foodstock.inventory.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
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

    @MockBean
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @MockBean
    private lateinit var getInventoryItemUseCase: GetInventoryItemUseCase

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private val userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
    private fun principal(id: UUID = userId) = RequestPostProcessor { request: MockHttpServletRequest ->
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = UsernamePasswordAuthenticationToken(
            AuthenticatedUser(userId = id, email = "user@test.com"), null, emptyList()
        )
        SecurityContextHolder.setContext(context)
        request
    }

    @Test
    fun `addItem returns created inventory item response`() {
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(addItemUseCase.addItem(any())).thenReturn(item)

        mockMvc.post("/api/v1/inventory") {
            with(principal())
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
            with(principal())
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
            with(principal())
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
            with(principal())
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

    @Test
    fun `getInventory returns all items for house`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryUseCase.getInventory(any(), anyOrNull(), any())).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory") {
            with(principal())
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(item.id.toString()) }
                jsonPath("$[0].houseId") { value(item.houseId.toString()) }
                jsonPath("$[0].quantityLevel") { value("PLENTY") }
            }
    }

    @Test
    fun `getInventory filters by quantityLevel`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.RUNNING_OUT)
        whenever(getInventoryUseCase.getInventory(any(), any(), any())).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory?quantityLevel=RUNNING_OUT") {
            with(principal())
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].quantityLevel") { value("RUNNING_OUT") }
            }
    }

    @Test
    fun `getInventory returns 400 for invalid quantityLevel value`() {
        mockMvc.get("/api/v1/inventory?quantityLevel=INVALID") {
            with(principal())
            header("X-House-Id", UUID.randomUUID().toString())
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventory returns 400 when X-House-Id header is missing`() {
        mockMvc.get("/api/v1/inventory") {
            with(principal())
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventoryItem returns item by id`() {
        val itemId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val item = inventoryItem(id = itemId, quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryItemUseCase.getInventoryItem(any(), any())).thenReturn(item)

        mockMvc.get("/api/v1/inventory/$itemId") {
            with(principal())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(itemId.toString()) }
                jsonPath("$.name") { value("Arroz") }
                jsonPath("$.quantityLevel") { value("PLENTY") }
            }
    }

    @Test
    fun `getInventoryItem returns 404 when item does not exist`() {
        val itemId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        whenever(getInventoryItemUseCase.getInventoryItem(any(), any())).thenThrow(ItemNotFoundException(itemId))

        mockMvc.get("/api/v1/inventory/$itemId") {
            with(principal())
        }
            .andExpect { status { isNotFound() } }
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
