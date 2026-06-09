# GET Shopping List Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/v1/shopping-lists` and `GET /api/v1/shopping-lists/{listId}` (with items nested inline) endpoints to the shopping bounded context.

**Architecture:** One new domain exception, two new use case interfaces in `domain/port/in`, one new response DTO (`ShoppingListDetailResponse` with nested items), implemented by `ShoppingListService`. No changes to `ShoppingConfig` — Spring resolves the single bean by interface type. New `@GetMapping` methods added to `ShoppingListController`.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, JUnit 5, Mockito Kotlin, MockMvc (`@WebMvcTest`)

**Branch:** `feat/get-shopping` (create a git worktree before starting)

---

## File Map

| Action | Path |
|--------|------|
| CREATE | `apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ShoppingListNotFoundException.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListsUseCase.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListUseCase.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListDetailResponse.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt` |

---

## Task 1: Create exception, use case interfaces, and response DTO

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ShoppingListNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListsUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListDetailResponse.kt`

- [ ] **Step 1: Create ShoppingListNotFoundException**

```kotlin
package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ShoppingListNotFoundException(listId: UUID) : ResourceNotFoundException("Shopping list not found: $listId")
```

- [ ] **Step 2: Create GetShoppingListsUseCase**

```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

interface GetShoppingListsUseCase {
    fun getShoppingLists(houseId: UUID): List<ShoppingList>
}
```

- [ ] **Step 3: Create GetShoppingListUseCase**

Returns a `Pair` of the list and its items — keeps the domain layer free of response DTOs.

```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

interface GetShoppingListUseCase {
    fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>>
}
```

- [ ] **Step 4: Create ShoppingListDetailResponse**

```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListItemSummary(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val inventoryItemId: UUID?,
    val createdAt: LocalDateTime
)

data class ShoppingListDetailResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val items: List<ShoppingListItemSummary>
)

fun Pair<ShoppingList, List<ShoppingListItem>>.toDetailResponse(): ShoppingListDetailResponse {
    val (list, items) = this
    return ShoppingListDetailResponse(
        id = list.id,
        houseId = list.houseId,
        name = list.name,
        status = list.status,
        createdBy = list.createdBy,
        createdAt = list.createdAt,
        updatedAt = list.updatedAt,
        items = items.map { item ->
            ShoppingListItemSummary(
                id = item.id,
                name = item.name,
                quantity = item.quantity,
                checked = item.checked,
                inventoryItemId = item.inventoryItemId,
                createdAt = item.createdAt
            )
        }
    )
}
```

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ShoppingListNotFoundException.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListsUseCase.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/GetShoppingListUseCase.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListDetailResponse.kt
git commit -m "feat(shopping): add ShoppingListNotFoundException, query use case interfaces, and detail DTO"
```

---

## Task 2: Implement query methods in ShoppingListService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt`

- [ ] **Step 1: Add failing tests to ShoppingListServiceTest**

Add these imports to the existing test class:

```kotlin
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingListItem
import org.junit.jupiter.api.assertThrows
```

Add these test methods:

```kotlin
    @Test
    fun `getShoppingLists returns lists for house`() {
        val houseId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val list = ShoppingList(
            id = UUID.randomUUID(), houseId = houseId, name = "Weekly",
            status = ShoppingListStatus.OPEN, createdBy = UUID.randomUUID(),
            createdAt = now, updatedAt = now
        )
        whenever(shoppingListRepository.findAllByHouseId(houseId)).thenReturn(listOf(list))

        val result = service.getShoppingLists(houseId)

        assertEquals(1, result.size)
        assertEquals(houseId, result[0].houseId)
        assertEquals("Weekly", result[0].name)
    }

    @Test
    fun `getShoppingLists returns empty list when house has no lists`() {
        val houseId = UUID.randomUUID()
        whenever(shoppingListRepository.findAllByHouseId(houseId)).thenReturn(emptyList())

        val result = service.getShoppingLists(houseId)

        assertEquals(0, result.size)
    }

    @Test
    fun `getShoppingList returns list with its items`() {
        val listId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val list = ShoppingList(
            id = listId, houseId = houseId, name = "Weekly",
            status = ShoppingListStatus.OPEN, createdBy = UUID.randomUUID(),
            createdAt = now, updatedAt = now
        )
        val item = ShoppingListItem(
            id = UUID.randomUUID(), shoppingListId = listId, inventoryItemId = null,
            name = "Milk", quantity = 1, checked = false, createdAt = now
        )
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(item))

        val (resultList, resultItems) = service.getShoppingList(listId)

        assertEquals(listId, resultList.id)
        assertEquals(1, resultItems.size)
        assertEquals("Milk", resultItems[0].name)
    }

    @Test
    fun `getShoppingList throws ShoppingListNotFoundException when list does not exist`() {
        val listId = UUID.randomUUID()
        whenever(shoppingListRepository.findById(listId)).thenReturn(null)

        assertThrows<ShoppingListNotFoundException> { service.getShoppingList(listId) }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.ShoppingListServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `getShoppingLists` and `getShoppingList` not found on `ShoppingListService`.

- [ ] **Step 3: Implement query methods in ShoppingListService**

Add these imports to `ShoppingListService.kt`:

```kotlin
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
```

Update the class declaration:

```kotlin
class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort,
    private val clock: Clock
) : GenerateShoppingListUseCase, GetShoppingListsUseCase, GetShoppingListUseCase {
```

Add these methods before the closing brace:

```kotlin
    override fun getShoppingLists(houseId: UUID): List<ShoppingList> =
        shoppingListRepository.findAllByHouseId(houseId)

    override fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>> {
        val list = shoppingListRepository.findById(listId)
            ?: throw ShoppingListNotFoundException(listId)
        val items = shoppingListRepository.findItemsByListId(listId)
        return Pair(list, items)
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.ShoppingListServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt
git commit -m "feat(shopping): implement getShoppingLists and getShoppingList in ShoppingListService"
```

---

## Task 3: Add GET endpoints to ShoppingListController (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt`

- [ ] **Step 1: Add failing tests to ShoppingListControllerTest**

Add these imports:

```kotlin
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import org.springframework.test.web.servlet.get
```

Add these two `@MockBean` fields after the existing one:

```kotlin
    @MockBean
    private lateinit var getShoppingListsUseCase: GetShoppingListsUseCase

    @MockBean
    private lateinit var getShoppingListUseCase: GetShoppingListUseCase
```

Add these test methods:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.ShoppingListControllerTest" 2>&1 | tail -20
```

Expected: compilation error — new use cases not in controller yet.

- [ ] **Step 3: Add GET endpoints and new dependencies to ShoppingListController**

Add these imports to `ShoppingListController.kt`:

```kotlin
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListDetailResponse
import com.foodstock.shopping.adapter.`in`.dto.toDetailResponse
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
```

Update the constructor:

```kotlin
@RestController
@RequestMapping("/api/v1/shopping-lists")
class ShoppingListController(
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val getShoppingListsUseCase: GetShoppingListsUseCase,
    private val getShoppingListUseCase: GetShoppingListUseCase
) {
```

Add these methods inside the class body (after the existing `generateList` method):

```kotlin
    @GetMapping
    fun getShoppingLists(
        @RequestHeader("X-House-Id") houseId: UUID
    ): List<ShoppingListResponse> =
        getShoppingListsUseCase.getShoppingLists(houseId).map { it.toResponse() }

    @GetMapping("/{listId}")
    fun getShoppingList(@PathVariable listId: UUID): ShoppingListDetailResponse =
        getShoppingListUseCase.getShoppingList(listId).toDetailResponse()
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.ShoppingListControllerTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Run full test suite**

```bash
cd apps/api && ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt
git commit -m "feat(shopping): add GET /shopping-lists and /shopping-lists/{id} endpoints"
```
