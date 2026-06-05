package com.foodstock.shopping.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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

        mockMvc.post("/api/v1/shopping-lists/generate") {
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
}
