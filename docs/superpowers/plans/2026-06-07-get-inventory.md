# GET Inventory Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/v1/inventory` (with optional `?quantityLevel=` filter) and `GET /api/v1/inventory/{itemId}` endpoints to the inventory bounded context.

**Architecture:** Two new use case interfaces in `domain/port/in`, implemented by `InventoryService`. No changes to `InventoryConfig` — Spring resolves the single `inventoryService` bean by interface type. New `@GetMapping` methods added to `InventoryController`. A null `quantityLevel` param delegates to `findAllByHouseId`; a non-null value delegates to `findAllByHouseIdAndQuantityLevel`.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, JUnit 5, Mockito Kotlin, MockMvc (`@WebMvcTest`)

**Branch:** `feat/get-inventory` (create a git worktree before starting)

---

## File Map

| Action | Path |
|--------|------|
| CREATE | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt` |

---

## Task 1: Create use case interfaces

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt`

- [ ] **Step 1: Create GetInventoryUseCase**

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

interface GetInventoryUseCase {
    fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?): List<InventoryItem>
}
```

- [ ] **Step 2: Create GetInventoryItemUseCase**

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import java.util.UUID

interface GetInventoryItemUseCase {
    fun getInventoryItem(itemId: UUID): InventoryItem
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt \
        apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt
git commit -m "feat(inventory): add GetInventory and GetInventoryItem use case interfaces"
```

---

## Task 2: Implement query methods in InventoryService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`

- [ ] **Step 1: Add failing tests to InventoryServiceTest**

Add these test methods to the existing `InventoryServiceTest` class:

```kotlin
@Test
fun `getInventory returns all items when no filter provided`() {
    val houseId = UUID.randomUUID()
    val item = InventoryItem(
        id = UUID.randomUUID(), houseId = houseId, name = "Arroz",
        category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
        expiryDate = null, notes = null,
        createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
    )
    whenever(inventoryRepository.findAllByHouseId(houseId)).thenReturn(listOf(item))

    val result = service.getInventory(houseId, null)

    assertEquals(1, result.size)
    assertEquals(item.id, result[0].id)
}

@Test
fun `getInventory filters by quantityLevel when provided`() {
    val houseId = UUID.randomUUID()
    val item = InventoryItem(
        id = UUID.randomUUID(), houseId = houseId, name = "Arroz",
        category = Category.FOOD, quantityLevel = QuantityLevel.RUNNING_OUT,
        expiryDate = null, notes = null,
        createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
    )
    whenever(inventoryRepository.findAllByHouseIdAndQuantityLevel(houseId, QuantityLevel.RUNNING_OUT))
        .thenReturn(listOf(item))

    val result = service.getInventory(houseId, QuantityLevel.RUNNING_OUT)

    assertEquals(1, result.size)
    assertEquals(QuantityLevel.RUNNING_OUT, result[0].quantityLevel)
}

@Test
fun `getInventoryItem returns item by id`() {
    val itemId = UUID.randomUUID()
    val item = InventoryItem(
        id = itemId, houseId = UUID.randomUUID(), name = "Arroz",
        category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
        expiryDate = null, notes = null,
        createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
    )
    whenever(inventoryRepository.findById(itemId)).thenReturn(item)

    val result = service.getInventoryItem(itemId)

    assertEquals(itemId, result.id)
}

@Test
fun `getInventoryItem throws ItemNotFoundException when item does not exist`() {
    val itemId = UUID.randomUUID()
    whenever(inventoryRepository.findById(itemId)).thenReturn(null)

    assertThrows<ItemNotFoundException> { service.getInventoryItem(itemId) }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `getInventory` and `getInventoryItem` not found on `InventoryService`.

- [ ] **Step 3: Implement query methods in InventoryService**

Add these imports to `InventoryService.kt`:

```kotlin
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.model.QuantityLevel
```

Update the class declaration:

```kotlin
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val clock: Clock
) : AddItemUseCase, UpdateItemQuantityUseCase, GetInventoryUseCase, GetInventoryItemUseCase {
```

Add these methods before the closing brace:

```kotlin
    override fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?): List<InventoryItem> =
        if (quantityLevel != null)
            inventoryRepository.findAllByHouseIdAndQuantityLevel(houseId, quantityLevel)
        else
            inventoryRepository.findAllByHouseId(houseId)

    override fun getInventoryItem(itemId: UUID): InventoryItem =
        inventoryRepository.findById(itemId) ?: throw ItemNotFoundException(itemId)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt \
        apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt
git commit -m "feat(inventory): implement getInventory and getInventoryItem in InventoryService"
```

---

## Task 3: Add GET endpoints to InventoryController (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`

- [ ] **Step 1: Add failing tests to InventoryControllerTest**

Add these imports to `InventoryControllerTest.kt`:

```kotlin
import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import org.springframework.test.web.servlet.get
```

Add these two `@MockBean` fields after the existing ones:

```kotlin
    @MockBean
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @MockBean
    private lateinit var getInventoryItemUseCase: GetInventoryItemUseCase
```

Add these test methods:

```kotlin
    @Test
    fun `getInventory returns all items for house`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryUseCase.getInventory(houseId, null)).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory") {
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(item.id.toString()) }
                jsonPath("$[0].houseId") { value(houseId.toString()) }
                jsonPath("$[0].quantityLevel") { value("PLENTY") }
            }
    }

    @Test
    fun `getInventory filters by quantityLevel`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.RUNNING_OUT)
        whenever(getInventoryUseCase.getInventory(houseId, QuantityLevel.RUNNING_OUT)).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory?quantityLevel=RUNNING_OUT") {
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
            header("X-House-Id", UUID.randomUUID().toString())
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventory returns 400 when X-House-Id header is missing`() {
        mockMvc.get("/api/v1/inventory")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventoryItem returns item by id`() {
        val itemId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val item = inventoryItem(id = itemId, quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryItemUseCase.getInventoryItem(itemId)).thenReturn(item)

        mockMvc.get("/api/v1/inventory/$itemId")
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
        whenever(getInventoryItemUseCase.getInventoryItem(itemId)).thenThrow(ItemNotFoundException(itemId))

        mockMvc.get("/api/v1/inventory/$itemId")
            .andExpect { status { isNotFound() } }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryControllerTest" 2>&1 | tail -20
```

Expected: compilation error — new use cases not in controller yet.

- [ ] **Step 3: Add GET endpoints and new dependencies to InventoryController**

Add these imports to `InventoryController.kt`:

```kotlin
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
```

Update the constructor:

```kotlin
@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val addItemUseCase: AddItemUseCase,
    private val updateItemQuantityUseCase: UpdateItemQuantityUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getInventoryItemUseCase: GetInventoryItemUseCase
) {
```

Add these methods inside the class body (after the existing `updateQuantity` method):

```kotlin
    @GetMapping
    fun getInventory(
        @RequestHeader("X-House-Id") houseId: UUID,
        @RequestParam(required = false) quantityLevel: QuantityLevel?
    ): List<InventoryItemResponse> =
        getInventoryUseCase.getInventory(houseId, quantityLevel).map { it.toResponse() }

    @GetMapping("/{itemId}")
    fun getInventoryItem(@PathVariable itemId: UUID): InventoryItemResponse =
        getInventoryItemUseCase.getInventoryItem(itemId).toResponse()
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryControllerTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Run full test suite**

```bash
cd apps/api && ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt \
        apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt
git commit -m "feat(inventory): add GET /inventory and /inventory/{itemId} endpoints"
```
