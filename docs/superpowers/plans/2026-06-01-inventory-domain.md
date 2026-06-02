# Inventory Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the inventory domain with hexagonal architecture — adding `AddItemUseCase`, `InventoryConfig` wiring, response DTOs, and full unit test coverage for `InventoryService`.

**Architecture:** Hexagonal (ports & adapters). Domain layer is framework-free: `InventoryService` is a plain class wired manually via `InventoryConfig` (same pattern as `HouseholdConfig`). Port/in interfaces drive the controller; port/out interfaces abstract JPA. All JPA entity mapping is in `adapter/out`.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, Spring Data JPA, JUnit 5, Mockito-Kotlin 5.2.1, JDK 21

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| **Create** | `domain/port/in/AddItemUseCase.kt` | Port/in — `AddItemCommand` data class + `AddItemUseCase` interface |
| **Modify** | `domain/service/InventoryService.kt` | Remove `@Service`, implement `AddItemUseCase`, keep `UpdateItemQuantityUseCase` |
| **Create** | `config/InventoryConfig.kt` | `@Configuration` — manually wire `InventoryService` as Spring `@Bean` |
| **Modify** | `adapter/in/InventoryController.kt` | Add `POST /api/v1/inventory` endpoint + `InventoryItemResponse`/`AddItemRequest` DTOs |
| **Create** | `src/test/…/InventoryServiceTest.kt` | Unit tests for all guard conditions and happy paths in `InventoryService` |

All paths relative to `apps/api/src/main/kotlin/com/foodstock/inventory/` unless noted.

---

## Task 1: AddItemUseCase port/in

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/AddItemUseCase.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.time.LocalDate
import java.util.UUID

data class AddItemCommand(
    val houseId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?
)

interface AddItemUseCase {
    fun addItem(command: AddItemCommand): InventoryItem
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/AddItemUseCase.kt
git commit -m "feat(inventory): add AddItemUseCase port/in"
```

---

## Task 2: Write failing unit tests for InventoryService

**Files:**
- Create: `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`

The tests are written first (TDD). They will fail until Task 3 is done because `InventoryService` does not yet implement `AddItemUseCase`.

- [ ] **Step 1: Create the test file**

```kotlin
package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    private val inventoryRepository: InventoryRepository = mock()
    private val service = InventoryService(inventoryRepository)

    // --- addItem ---

    @Test
    fun `addItem saves item with generated id and timestamps`() {
        val houseId = UUID.randomUUID()
        val command = AddItemCommand(
            houseId = houseId,
            name = "Arroz",
            category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY,
            expiryDate = LocalDate.of(2026, 12, 31),
            notes = null
        )
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(houseId, result.houseId)
        assertEquals("Arroz", result.name)
        assertEquals(Category.FOOD, result.category)
        assertEquals(QuantityLevel.PLENTY, result.quantityLevel)
        assertEquals(LocalDate.of(2026, 12, 31), result.expiryDate)
        assertNotNull(result.id)
        assertNotNull(result.createdAt)
        assertNotNull(result.updatedAt)
        assertEquals(result.createdAt, result.updatedAt)
    }

    @Test
    fun `addItem saves item without optional fields`() {
        val command = AddItemCommand(
            houseId = UUID.randomUUID(),
            name = "Sabão",
            category = Category.CLEANING,
            quantityLevel = QuantityLevel.RUNNING_OUT,
            expiryDate = null,
            notes = null
        )
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(null, result.expiryDate)
        assertEquals(null, result.notes)
        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
    }

    // --- updateQuantity ---

    @Test
    fun `updateQuantity updates quantityLevel on existing item`() {
        val itemId = UUID.randomUUID()
        val existing = InventoryItem(
            id = itemId, houseId = UUID.randomUUID(), name = "Leite",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(existing)
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.RUNNING_OUT)
        )

        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
        assertEquals(itemId, result.id)

        val captor = argumentCaptor<InventoryItem>()
        verify(inventoryRepository).save(captor.capture())
        assertEquals(QuantityLevel.RUNNING_OUT, captor.firstValue.quantityLevel)
    }

    @Test
    fun `updateQuantity throws NoSuchElementException when item not found`() {
        val itemId = UUID.randomUUID()
        whenever(inventoryRepository.findById(itemId)).thenReturn(null)

        assertThrows<NoSuchElementException> {
            service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.ENOUGH))
        }
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure (AddItemUseCase not yet implemented)**

```bash
cd apps/api && ./gradlew test --tests "com.foodstock.inventory.domain.service.InventoryServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `InventoryService` does not implement `AddItemUseCase` / `addItem` not found.

- [ ] **Step 3: Commit failing tests**

```bash
git add apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt
git commit -m "test(inventory): add failing InventoryServiceTest"
```

---

## Task 3: Extend InventoryService — implement AddItemUseCase, remove @Service

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`

- [ ] **Step 1: Replace InventoryService.kt**

```kotlin
package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import com.foodstock.inventory.domain.port.out.InventoryRepository
import java.time.LocalDateTime
import java.util.UUID

class InventoryService(
    private val inventoryRepository: InventoryRepository
) : AddItemUseCase, UpdateItemQuantityUseCase {

    override fun addItem(command: AddItemCommand): InventoryItem {
        val now = LocalDateTime.now()
        val item = InventoryItem(
            id = UUID.randomUUID(),
            houseId = command.houseId,
            name = command.name,
            category = command.category,
            quantityLevel = command.quantityLevel,
            expiryDate = command.expiryDate,
            notes = command.notes,
            createdAt = now,
            updatedAt = now
        )
        return inventoryRepository.save(item)
    }

    override fun updateQuantity(command: UpdateItemQuantityCommand): InventoryItem {
        val item = inventoryRepository.findById(command.itemId)
            ?: throw NoSuchElementException("Item not found: ${command.itemId}")
        val updated = item.copy(
            quantityLevel = command.quantityLevel,
            updatedAt = LocalDateTime.now()
        )
        return inventoryRepository.save(updated)
    }
}
```

- [ ] **Step 2: Run tests — expect all 4 to pass**

```bash
cd apps/api && ./gradlew test --tests "com.foodstock.inventory.domain.service.InventoryServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt
git commit -m "feat(inventory): implement AddItemUseCase in InventoryService, remove @Service"
```

---

## Task 4: InventoryConfig — Spring wiring

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.foodstock.inventory.config

import com.foodstock.inventory.adapter.out.InventoryJpaRepository
import com.foodstock.inventory.domain.service.InventoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InventoryConfig(
    private val inventoryJpaRepository: InventoryJpaRepository
) {
    @Bean
    fun inventoryService(): InventoryService = InventoryService(
        inventoryRepository = inventoryJpaRepository
    )
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt
git commit -m "feat(inventory): add InventoryConfig Spring wiring"
```

---

## Task 5: Add POST endpoint and response DTOs to InventoryController

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`

Response DTOs are co-located in the controller file (same pattern as `HouseController`). The controller now depends on both `AddItemUseCase` and `UpdateItemQuantityUseCase`.

- [ ] **Step 1: Replace InventoryController.kt**

```kotlin
package com.foodstock.inventory.adapter.`in`

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AddItemRequest(
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
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
        @RequestBody request: AddItemRequest,
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
        return InventoryItemResponse(
            id = item.id, houseId = item.houseId, name = item.name,
            category = item.category, quantityLevel = item.quantityLevel,
            expiryDate = item.expiryDate, notes = item.notes,
            createdAt = item.createdAt, updatedAt = item.updatedAt
        )
    }

    @PatchMapping("/{itemId}/quantity")
    fun updateQuantity(
        @PathVariable itemId: UUID,
        @RequestBody request: UpdateQuantityRequest
    ): InventoryItemResponse {
        val item = updateItemQuantityUseCase.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = request.quantityLevel)
        )
        return InventoryItemResponse(
            id = item.id, houseId = item.houseId, name = item.name,
            category = item.category, quantityLevel = item.quantityLevel,
            expiryDate = item.expiryDate, notes = item.notes,
            createdAt = item.createdAt, updatedAt = item.updatedAt
        )
    }
}
```

- [ ] **Step 2: Run full test suite to confirm no regressions**

```bash
cd apps/api && ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt
git commit -m "feat(inventory): add POST /inventory endpoint and InventoryItemResponse DTO"
```

---

## Self-Review

### Spec coverage

| Requirement | Covered by |
|---|---|
| `InventoryItem` domain model with `QuantityLevel` enum | Already on `main` — `InventoryItem.kt` |
| `AddItemUseCase` port/in | Task 1 |
| `UpdateQuantityUseCase` port/in | Already on `main` — `UpdateItemQuantityUseCase.kt` |
| `InventoryService` implements both use cases | Task 3 |
| `InventoryConfig` Spring wiring (no `@Service` on service) | Task 4 |
| `adapter/in` — REST controller with both endpoints | Task 5 |
| `adapter/out` — JPA entity + repository | Already on `main` — `InventoryItemJpaEntity.kt`, `InventoryJpaRepository.kt` |
| Response DTOs (not returning raw domain models) | Task 5 |
| Unit tests for `InventoryService` | Task 2 |

### Type consistency check

- `AddItemCommand` defined in Task 1, used in Task 3 (`InventoryService.addItem`) and Task 5 (`InventoryController.addItem`) — consistent.
- `InventoryService` constructor takes `InventoryRepository` in Task 3 — same as `InventoryConfig` in Task 4 — consistent.
- `InventoryJpaRepository` in Task 4 implements `InventoryRepository` (already on `main`) — consistent.
- `UpdateItemQuantityCommand` already on `main`, referenced unchanged in Task 3 and Task 5 — consistent.
- `InventoryItemResponse` defined and used only in Task 5 — consistent.

No gaps found.
