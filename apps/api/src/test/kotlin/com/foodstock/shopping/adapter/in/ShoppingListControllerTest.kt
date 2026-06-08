package com.foodstock.shopping.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListResponse
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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
}
