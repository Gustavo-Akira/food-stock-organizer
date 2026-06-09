package com.foodstock.shopping.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListResponse
import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingItemNotFoundException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(ShoppingListController::class)
@AutoConfigureMockMvc(addFilters = false)
class ShoppingListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var generateShoppingListUseCase: GenerateShoppingListUseCase

    @MockBean
    private lateinit var getShoppingListsUseCase: GetShoppingListsUseCase

    @MockBean
    private lateinit var getShoppingListUseCase: GetShoppingListUseCase

    @MockBean
    private lateinit var startShoppingUseCase: StartShoppingUseCase

    @MockBean
    private lateinit var completeShoppingUseCase: CompleteShoppingUseCase

    @MockBean
    private lateinit var cancelShoppingUseCase: CancelShoppingUseCase

    @MockBean
    private lateinit var addShoppingItemUseCase: AddShoppingItemUseCase

    @MockBean
    private lateinit var removeShoppingItemUseCase: RemoveShoppingItemUseCase

    @MockBean
    private lateinit var updateShoppingItemUseCase: UpdateShoppingItemUseCase

    // --- Existing tests ---

    @Test
    fun `generateList returns created shopping list`() {
        val listId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val houseId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        val now = LocalDateTime.parse("2026-06-05T12:00:00")
        whenever(generateShoppingListUseCase.generateFromRunningOutItems(any())).thenReturn(
            ShoppingList(
                id = listId,
                houseId = houseId,
                name = "Lista semanal",
                status = ShoppingListStatus.OPEN,
                createdBy = userId,
                createdAt = now,
                updatedAt = now
            )
        )

        val result = mockMvc.post("/api/v1/shopping-lists/generate") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            content = objectMapper.writeValueAsString(
                GenerateShoppingListRequest(houseId = houseId, listName = "Lista semanal")
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(listId.toString()) }
                jsonPath("$.houseId") { value(houseId.toString()) }
                jsonPath("$.name") { value("Lista semanal") }
                jsonPath("$.status") { value("OPEN") }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, ShoppingListResponse::class.java)
        assert(response.id == listId)
        assert(response.name == "Lista semanal")
    }

    @Test
    fun `generateList rejects missing user header`() {
        mockMvc.post("/api/v1/shopping-lists/generate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                GenerateShoppingListRequest(houseId = UUID.randomUUID(), listName = "Lista semanal")
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `getShoppingLists returns lists for house`() {
        val houseId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val listId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val createdBy = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        whenever(getShoppingListsUseCase.getShoppingLists(houseId)).thenReturn(
            listOf(ShoppingList(id = listId, houseId = houseId, name = "Weekly", status = ShoppingListStatus.OPEN, createdBy = createdBy, createdAt = now, updatedAt = now))
        )

        mockMvc.get("/api/v1/shopping-lists") {
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(listId.toString()) }
                jsonPath("$[0].name") { value("Weekly") }
                jsonPath("$[0].status") { value("OPEN") }
            }
    }

    @Test
    fun `getShoppingLists returns 400 when X-House-Id header is missing`() {
        mockMvc.get("/api/v1/shopping-lists")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getShoppingList returns list with nested items`() {
        val listId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val houseId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val itemId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val createdBy = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        val list = ShoppingList(id = listId, houseId = houseId, name = "Weekly", status = ShoppingListStatus.OPEN, createdBy = createdBy, createdAt = now, updatedAt = now)
        val item = ShoppingListItem(id = itemId, shoppingListId = listId, inventoryItemId = null, name = "Milk", quantity = 2, checked = false, createdAt = now)
        whenever(getShoppingListUseCase.getShoppingList(listId)).thenReturn(Pair(list, listOf(item)))

        mockMvc.get("/api/v1/shopping-lists/$listId")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(listId.toString()) }
                jsonPath("$.name") { value("Weekly") }
                jsonPath("$.items[0].id") { value(itemId.toString()) }
                jsonPath("$.items[0].name") { value("Milk") }
                jsonPath("$.items[0].quantity") { value(2) }
                jsonPath("$.items[0].checked") { value(false) }
            }
    }

    @Test
    fun `getShoppingList returns 404 when list does not exist`() {
        val listId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(getShoppingListUseCase.getShoppingList(listId)).thenThrow(ShoppingListNotFoundException(listId))

        mockMvc.get("/api/v1/shopping-lists/$listId")
            .andExpect { status { isNotFound() } }
    }

    // --- State transition tests ---

    @Test
    fun `start returns 200 with SHOPPING status`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val houseId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val now = LocalDateTime.parse("2026-06-07T20:00:00")
        whenever(startShoppingUseCase.start(any())).thenReturn(
            ShoppingList(listId, houseId, "Weekly", ShoppingListStatus.SHOPPING, userId, now, now, version = 1)
        )

        mockMvc.post("/api/v1/shopping-lists/$listId/start") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("SHOPPING") }
                jsonPath("$.version") { value(1) }
            }
    }

    @Test
    fun `start returns 404 when list not found`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(startShoppingUseCase.start(any())).thenThrow(ShoppingListNotFoundException(listId))

        mockMvc.post("/api/v1/shopping-lists/$listId/start") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `start returns 409 when list is in wrong state`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(startShoppingUseCase.start(any())).thenThrow(InvalidShoppingListStateException("Cannot start"))

        mockMvc.post("/api/v1/shopping-lists/$listId/start") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `start returns 403 when caller is not owner`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(startShoppingUseCase.start(any())).thenThrow(ShoppingListAccessDeniedException("Not owner"))

        mockMvc.post("/api/v1/shopping-lists/$listId/start") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect { status { isForbidden() } }
    }

    @Test
    fun `complete returns 200 with COMPLETED status`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val houseId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val now = LocalDateTime.parse("2026-06-07T20:00:00")
        whenever(completeShoppingUseCase.complete(any())).thenReturn(
            ShoppingList(listId, houseId, "Weekly", ShoppingListStatus.COMPLETED, userId, now, now, version = 2)
        )

        mockMvc.post("/api/v1/shopping-lists/$listId/complete") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "1")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("COMPLETED") }
            }
    }

    @Test
    fun `cancel returns 200 with CANCELLED status`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val houseId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val now = LocalDateTime.parse("2026-06-07T20:00:00")
        whenever(cancelShoppingUseCase.cancel(any())).thenReturn(
            ShoppingList(listId, houseId, "Weekly", ShoppingListStatus.CANCELLED, userId, now, now, version = 1)
        )

        mockMvc.post("/api/v1/shopping-lists/$listId/cancel") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("CANCELLED") }
            }
    }

    // --- Item mutation tests ---

    @Test
    fun `addItem returns 201 with new item`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val now = LocalDateTime.parse("2026-06-07T20:00:00")
        val item = ShoppingListItem(itemId, listId, null, "Bread", 2, false, now)
        whenever(addShoppingItemUseCase.addItem(any())).thenReturn(item)

        mockMvc.post("/api/v1/shopping-lists/$listId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
            content = objectMapper.writeValueAsString(mapOf("name" to "Bread", "quantity" to 2))
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(itemId.toString()) }
                jsonPath("$.name") { value("Bread") }
                jsonPath("$.quantity") { value(2) }
                jsonPath("$.checked") { value(false) }
            }
    }

    @Test
    fun `addItem returns 409 when list is in terminal state`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(addShoppingItemUseCase.addItem(any())).thenThrow(InvalidShoppingListStateException("Cannot modify"))

        mockMvc.post("/api/v1/shopping-lists/$listId/items") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
            content = objectMapper.writeValueAsString(mapOf("name" to "Bread", "quantity" to 1))
        }
            .andExpect { status { isConflict() } }
    }

    @Test
    fun `removeItem returns 204`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

        mockMvc.delete("/api/v1/shopping-lists/$listId/items/$itemId") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect { status { isNoContent() } }

        verify(removeShoppingItemUseCase).removeItem(any())
    }

    @Test
    fun `removeItem returns 404 when item not found`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(removeShoppingItemUseCase.removeItem(any())).thenThrow(ShoppingItemNotFoundException(itemId))

        mockMvc.delete("/api/v1/shopping-lists/$listId/items/$itemId") {
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
        }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `updateItem returns 200 with updated fields`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val now = LocalDateTime.parse("2026-06-07T20:00:00")
        val item = ShoppingListItem(itemId, listId, null, "Bread", 3, true, now)
        whenever(updateShoppingItemUseCase.updateItem(any())).thenReturn(item)

        mockMvc.patch("/api/v1/shopping-lists/$listId/items/$itemId") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            header("X-List-Version", "1")
            content = objectMapper.writeValueAsString(mapOf("quantity" to 3, "checked" to true))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.quantity") { value(3) }
                jsonPath("$.checked") { value(true) }
            }
    }

    @Test
    fun `updateItem returns 400 when body has no fields`() {
        val listId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val itemId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")

        mockMvc.patch("/api/v1/shopping-lists/$listId/items/$itemId") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            header("X-List-Version", "0")
            content = "{}"
        }
            .andExpect { status { isBadRequest() } }
    }
}
