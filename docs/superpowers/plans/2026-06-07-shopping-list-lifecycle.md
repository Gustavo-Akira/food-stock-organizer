# Shopping List Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement state transitions (OPEN→SHOPPING→COMPLETED, soft cancel), full item CRUD, inventory restock on completion, and optimistic locking for shopping lists.

**Architecture:** Hexagonal (ports & adapters) under `com.foodstock.shopping`. Six new use case interfaces (port/in), two new anti-corruption ports (port/out) bridging to household and inventory domains, all implemented by `ShoppingListService`. `X-List-Version` header carries the optimistic lock token; `@Version` on the JPA entity enforces it at DB level.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, Spring Data JPA, Mockito-Kotlin (tests), MockMvc (`@WebMvcTest`), Flyway migrations.

> **Spec note:** `MemberRole` in the household domain has only `OWNER` and `MEMBER` — no `ADMIN` exists. All "OWNER or ADMIN" references in the spec reduce to **OWNER only**.

---

## File Map

### New files
| File | Purpose |
|---|---|
| `apps/api/src/main/resources/db/migration/V2__add_shopping_list_version.sql` | Add `version` column to `shopping_lists` |
| `shopping/domain/exception/InvalidShoppingListStateException.kt` | 409 — illegal state transition or item mutation on terminal list |
| `shopping/domain/exception/ShoppingItemNotFoundException.kt` | 404 — item not found or belongs to another list |
| `shopping/domain/exception/ShoppingListAccessDeniedException.kt` | 403 — not an active member / not owner |
| `shopping/domain/port/in/StartShoppingUseCase.kt` | Command + interface for OPEN→SHOPPING |
| `shopping/domain/port/in/CompleteShoppingUseCase.kt` | Command + interface for SHOPPING→COMPLETED |
| `shopping/domain/port/in/CancelShoppingUseCase.kt` | Command + interface for →CANCELLED |
| `shopping/domain/port/in/AddShoppingItemUseCase.kt` | Command + interface to add an item |
| `shopping/domain/port/in/RemoveShoppingItemUseCase.kt` | Command + interface to remove an item |
| `shopping/domain/port/in/UpdateShoppingItemUseCase.kt` | Command + interface to update quantity/checked |
| `shopping/domain/port/out/MemberRolePort.kt` | Anti-corruption port: shopping's view of house membership |
| `shopping/domain/port/out/RestockItemsPort.kt` | Anti-corruption port: shopping's view of inventory restock |
| `shopping/adapter/out/MemberRoleAdapter.kt` | Bridges to `HouseMemberRepository` (household domain) |
| `shopping/adapter/out/InventoryRestockAdapter.kt` | Bridges to `InventoryRepository` (inventory domain) |

All paths are relative to `apps/api/src/main/kotlin/com/foodstock/`.

### Modified files
| File | Change |
|---|---|
| `shopping/domain/model/ShoppingList.kt` | Add `CANCELLED` status; add `version: Long = 0` field |
| `shopping/adapter/out/ShoppingListJpaEntity.kt` | Add `@Version val version: Long = 0` |
| `shopping/adapter/in/dto/ShoppingListResponse.kt` | Add `version: Long` field |
| `shopping/domain/port/out/ShoppingListRepository.kt` | Add `update`, `findItemById`, `updateItem`, `deleteItem` |
| `shopping/adapter/out/ShoppingListJpaRepository.kt` | Implement new repo methods; filter CANCELLED from list query |
| `shopping/domain/service/ShoppingListService.kt` | Add 6 new use cases + private helpers |
| `shopping/config/ShoppingConfig.kt` | Wire `MemberRoleAdapter`, `InventoryRestockAdapter` |
| `shopping/adapter/in/ShoppingListController.kt` | Add 6 new endpoints |
| `shopping/adapter/in/dto/ShoppingListItemResponse.kt` | New file — item DTO + extension |
| `shopping/adapter/in/dto/AddItemRequest.kt` | New file — POST body DTO |
| `shopping/adapter/in/dto/UpdateItemRequest.kt` | New file — PATCH body DTO |
| `common/GlobalExceptionHandler.kt` | Add handlers for new exceptions + optimistic lock |
| `inventory/domain/port/out/InventoryRepository.kt` | Add `updateQuantityLevel` |
| `inventory/adapter/out/InventoryJpaRepository.kt` | Implement `updateQuantityLevel` via `@Modifying @Query` |
| `test/.../ShoppingListServiceTest.kt` | Add state transition + item mutation tests |
| `test/.../ShoppingListControllerTest.kt` | Add endpoint tests for all 6 new routes |
| `test/.../GlobalExceptionHandlerTest.kt` | Add tests for new exception mappings |

---

## Task 1: Flyway Migration

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V2__add_shopping_list_version.sql`

- [ ] **Step 1: Create the migration file**

```sql
ALTER TABLE shopping_lists ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

- [ ] **Step 2: Run existing tests to confirm the migration is picked up and tests still pass**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListJpaEntityTest"
```

Expected: PASS (existing entity test unaffected; migration applied by Flyway in-memory)

- [ ] **Step 3: Commit**

```
git add apps/api/src/main/resources/db/migration/V2__add_shopping_list_version.sql
git commit -m "feat(shopping): add version column for optimistic locking"
```

---

## Task 2: Domain Model + JPA Entity + Response DTO

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/model/ShoppingList.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/ShoppingListJpaEntity.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListResponse.kt`

- [ ] **Step 1: Update `ShoppingList.kt` — add `CANCELLED` and `version`**

```kotlin
package com.foodstock.shopping.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class ShoppingListStatus { OPEN, SHOPPING, COMPLETED, CANCELLED }

data class ShoppingList(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long = 0
)
```

- [ ] **Step 2: Update `ShoppingListJpaEntity.kt` — add `@Version`**

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "shopping_lists")
class ShoppingListJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "house_id", nullable = false)
    val houseId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ShoppingListStatus = ShoppingListStatus.OPEN,

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime,

    @Version
    val version: Long = 0
) {
    fun toDomain(): ShoppingList = ShoppingList(
        id = id, houseId = houseId, name = name, status = status,
        createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt, version = version
    )

    companion object {
        fun fromDomain(list: ShoppingList) = ShoppingListJpaEntity(
            id = list.id, houseId = list.houseId, name = list.name, status = list.status,
            createdBy = list.createdBy, createdAt = list.createdAt, updatedAt = list.updatedAt,
            version = list.version
        )
    }
}
```

- [ ] **Step 3: Update `ShoppingListResponse.kt` — expose `version`**

```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val status: ShoppingListStatus,
    val createdBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Long
)

fun ShoppingList.toResponse() = ShoppingListResponse(
    id = id, houseId = houseId, name = name, status = status,
    createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt, version = version
)
```

- [ ] **Step 4: Run all shopping tests to confirm nothing broken**

```
cd apps/api && ./gradlew test --tests "*.shopping.*"
```

Expected: all existing tests PASS (`version` defaults to `0` so no existing construction sites break)

- [ ] **Step 5: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/model/ShoppingList.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/ShoppingListJpaEntity.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListResponse.kt
git commit -m "feat(shopping): add CANCELLED status and version field for optimistic locking"
```

---

## Task 3: New Exceptions + GlobalExceptionHandler

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/InvalidShoppingListStateException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ShoppingItemNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ShoppingListAccessDeniedException.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/common/GlobalExceptionHandler.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/common/GlobalExceptionHandlerTest.kt`

- [ ] **Step 1: Create the three new exception classes**

```kotlin
// InvalidShoppingListStateException.kt
package com.foodstock.shopping.domain.exception

class InvalidShoppingListStateException(message: String) : RuntimeException(message)
```

```kotlin
// ShoppingItemNotFoundException.kt
package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ShoppingItemNotFoundException(itemId: UUID) : ResourceNotFoundException("Shopping item not found: $itemId")
```

```kotlin
// ShoppingListAccessDeniedException.kt
package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ForbiddenOperationException

class ShoppingListAccessDeniedException(message: String) : ForbiddenOperationException(message)
```

- [ ] **Step 2: Write failing tests in `GlobalExceptionHandlerTest.kt`**

Add these three test methods to the existing test class:

```kotlin
@Test
fun `returns 409 for InvalidShoppingListStateException`() {
    mockMvc.perform(
        MockMvcRequestBuilders.get("/test/invalid-state")
    ).andExpect(MockMvcResultMatchers.status().isConflict)
}

@Test
fun `returns 409 for OptimisticLockException`() {
    mockMvc.perform(
        MockMvcRequestBuilders.get("/test/optimistic-lock")
    ).andExpect(MockMvcResultMatchers.status().isConflict)
}

@Test
fun `returns 404 for ShoppingItemNotFoundException`() {
    mockMvc.perform(
        MockMvcRequestBuilders.get("/test/item-not-found")
    ).andExpect(MockMvcResultMatchers.status().isNotFound)
}
```

> Note: the test controller that powers `/test/**` routes is defined in the existing test class — add your three test endpoints to it.

- [ ] **Step 3: Run to confirm the three tests fail**

```
cd apps/api && ./gradlew test --tests "*.GlobalExceptionHandlerTest"
```

Expected: FAIL (handlers not registered yet)

- [ ] **Step 4: Add handlers to `GlobalExceptionHandler.kt`**

```kotlin
package com.foodstock.common

import com.foodstock.common.exception.ForbiddenOperationException
import com.foodstock.common.exception.InvalidOperationException
import com.foodstock.common.exception.ResourceNotFoundException
import com.foodstock.common.exception.UnauthorizedException
import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import jakarta.persistence.OptimisticLockException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidOperationException::class)
    fun handleBadRequest(ex: InvalidOperationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to ex.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidShoppingListStateException::class)
    fun handleShoppingListStateConflict(ex: InvalidShoppingListStateException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))

    @ExceptionHandler(OptimisticLockException::class)
    fun handleJpaOptimisticLock(ex: OptimisticLockException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Resource was modified concurrently. Please retry."))

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleSpringOptimisticLock(ex: ObjectOptimisticLockingFailureException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Resource was modified concurrently. Please retry."))
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```
cd apps/api && ./gradlew test --tests "*.GlobalExceptionHandlerTest"
```

Expected: all PASS

- [ ] **Step 6: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/exception/ \
        apps/api/src/main/kotlin/com/foodstock/common/GlobalExceptionHandler.kt \
        apps/api/src/test/kotlin/com/foodstock/common/GlobalExceptionHandlerTest.kt
git commit -m "feat(shopping): add lifecycle exceptions and register 409/optimistic lock handlers"
```

---

## Task 4: Repository Port Extensions + JPA Implementation

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/ShoppingListRepository.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/ShoppingListJpaRepository.kt`

- [ ] **Step 1: Extend `ShoppingListRepository` with four new methods**

```kotlin
package com.foodstock.shopping.domain.port.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

interface ShoppingListRepository {
    fun save(list: ShoppingList): ShoppingList
    fun update(list: ShoppingList): ShoppingList
    fun saveItem(item: ShoppingListItem): ShoppingListItem
    fun updateItem(item: ShoppingListItem): ShoppingListItem
    fun deleteItem(itemId: UUID)
    fun findById(id: UUID): ShoppingList?
    fun findItemById(itemId: UUID): ShoppingListItem?
    fun findAllByHouseId(houseId: UUID): List<ShoppingList>
    fun findItemsByListId(listId: UUID): List<ShoppingListItem>
}
```

- [ ] **Step 2: Update `ShoppingListJpaRepository.kt`**

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface ShoppingListJpaRepositoryDelegate : JpaRepository<ShoppingListJpaEntity, UUID> {
    fun findAllByHouseIdAndStatusNot(houseId: UUID, status: ShoppingListStatus): List<ShoppingListJpaEntity>
}

@Repository
class ShoppingListJpaRepository(
    private val delegate: ShoppingListJpaRepositoryDelegate,
    private val itemDelegate: ShoppingListItemJpaRepositoryDelegate
) : ShoppingListRepository {

    override fun save(list: ShoppingList): ShoppingList =
        delegate.save(ShoppingListJpaEntity.fromDomain(list)).toDomain()

    override fun update(list: ShoppingList): ShoppingList =
        delegate.save(ShoppingListJpaEntity.fromDomain(list)).toDomain()

    override fun saveItem(item: ShoppingListItem): ShoppingListItem =
        itemDelegate.save(ShoppingListItemJpaEntity.fromDomain(item)).toDomain()

    override fun updateItem(item: ShoppingListItem): ShoppingListItem =
        itemDelegate.save(ShoppingListItemJpaEntity.fromDomain(item)).toDomain()

    override fun deleteItem(itemId: UUID) =
        itemDelegate.deleteById(itemId)

    override fun findById(id: UUID): ShoppingList? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun findItemById(itemId: UUID): ShoppingListItem? =
        itemDelegate.findById(itemId).orElse(null)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<ShoppingList> =
        delegate.findAllByHouseIdAndStatusNot(houseId, ShoppingListStatus.CANCELLED).map { it.toDomain() }

    override fun findItemsByListId(listId: UUID): List<ShoppingListItem> =
        itemDelegate.findAllByShoppingListId(listId).map { it.toDomain() }
}
```

- [ ] **Step 3: Run all shopping tests to confirm compilation and existing tests still pass**

```
cd apps/api && ./gradlew test --tests "*.shopping.*"
```

Expected: PASS

- [ ] **Step 4: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/ShoppingListRepository.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/ShoppingListJpaRepository.kt
git commit -m "feat(shopping): extend repository with update, item CRUD, and CANCELLED filter"
```

---

## Task 5: MemberRolePort + MemberRoleAdapter

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/MemberRolePort.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/MemberRoleAdapter.kt`

- [ ] **Step 1: Create `MemberRolePort.kt`**

```kotlin
package com.foodstock.shopping.domain.port.out

import java.util.UUID

enum class HouseRole { OWNER, MEMBER }

interface MemberRolePort {
    fun getRole(houseId: UUID, userId: UUID): HouseRole?
}
```

- [ ] **Step 2: Write a failing unit test for `MemberRoleAdapter`**

Create `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/out/MemberRoleAdapterTest.kt`:

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.shopping.domain.port.out.HouseRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MemberRoleAdapterTest {

    private val houseMemberRepository: HouseMemberRepository = mock()
    private val adapter = MemberRoleAdapter(houseMemberRepository)

    @Test
    fun `returns OWNER for active owner`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertEquals(HouseRole.OWNER, adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns MEMBER for active member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertEquals(HouseRole.MEMBER, adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns null when member not found`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

        assertNull(adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns null when member is PENDING`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.MEMBER, MemberStatus.PENDING, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertNull(adapter.getRole(houseId, userId))
    }
}
```

- [ ] **Step 3: Run the test to confirm it fails**

```
cd apps/api && ./gradlew test --tests "*.MemberRoleAdapterTest"
```

Expected: FAIL (class does not exist yet)

- [ ] **Step 4: Create `MemberRoleAdapter.kt`**

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MemberRoleAdapter(
    private val houseMemberRepository: HouseMemberRepository
) : MemberRolePort {

    override fun getRole(houseId: UUID, userId: UUID): HouseRole? {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId) ?: return null
        if (member.status != MemberStatus.ACTIVE) return null
        return when (member.role) {
            MemberRole.OWNER -> HouseRole.OWNER
            MemberRole.MEMBER -> HouseRole.MEMBER
        }
    }
}
```

- [ ] **Step 5: Run tests to confirm they pass**

```
cd apps/api && ./gradlew test --tests "*.MemberRoleAdapterTest"
```

Expected: all PASS

- [ ] **Step 6: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/MemberRolePort.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/MemberRoleAdapter.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/adapter/out/MemberRoleAdapterTest.kt
git commit -m "feat(shopping): add MemberRolePort anti-corruption port and adapter"
```

---

## Task 6: RestockItemsPort + InventoryRestockAdapter + InventoryRepository Extension

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/RestockItemsPort.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/InventoryRestockAdapter.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/out/InventoryRepository.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/out/InventoryJpaRepository.kt`

- [ ] **Step 1: Create `RestockItemsPort.kt`**

```kotlin
package com.foodstock.shopping.domain.port.out

import java.util.UUID

interface RestockItemsPort {
    fun restock(itemIds: List<UUID>)
}
```

- [ ] **Step 2: Add `updateQuantityLevel` to `InventoryRepository`**

```kotlin
package com.foodstock.inventory.domain.port.out

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

interface InventoryRepository {
    fun save(item: InventoryItem): InventoryItem
    fun findById(id: UUID): InventoryItem?
    fun findAllByHouseId(houseId: UUID): List<InventoryItem>
    fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItem>
    fun deleteById(id: UUID)
    fun updateQuantityLevel(id: UUID, level: QuantityLevel)
}
```

- [ ] **Step 3: Implement `updateQuantityLevel` in `InventoryJpaRepository.kt`**

```kotlin
package com.foodstock.inventory.adapter.out

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface InventoryJpaRepositoryDelegate : JpaRepository<InventoryItemJpaEntity, UUID> {
    fun findAllByHouseId(houseId: UUID): List<InventoryItemJpaEntity>
    fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItemJpaEntity>

    @Modifying
    @Transactional
    @Query("UPDATE InventoryItemJpaEntity i SET i.quantityLevel = :level WHERE i.id = :id")
    fun updateQuantityLevelById(@Param("id") id: UUID, @Param("level") level: QuantityLevel)
}

@Repository
class InventoryJpaRepository(
    private val delegate: InventoryJpaRepositoryDelegate
) : InventoryRepository {

    override fun save(item: InventoryItem): InventoryItem =
        delegate.save(InventoryItemJpaEntity.fromDomain(item)).toDomain()

    override fun findById(id: UUID): InventoryItem? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<InventoryItem> =
        delegate.findAllByHouseId(houseId).map { it.toDomain() }

    override fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItem> =
        delegate.findAllByHouseIdAndQuantityLevel(houseId, level).map { it.toDomain() }

    override fun deleteById(id: UUID) =
        delegate.deleteById(id)

    override fun updateQuantityLevel(id: UUID, level: QuantityLevel) =
        delegate.updateQuantityLevelById(id, level)
}
```

- [ ] **Step 4: Write a failing unit test for `InventoryRestockAdapter`**

Create `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/out/InventoryRestockAdapterTest.kt`:

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryRestockAdapterTest {

    private val inventoryRepository: InventoryRepository = mock()
    private val adapter = InventoryRestockAdapter(inventoryRepository)

    @Test
    fun `restock calls updateQuantityLevel for each item id`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        adapter.restock(listOf(id1, id2))

        verify(inventoryRepository).updateQuantityLevel(id1, QuantityLevel.ENOUGH)
        verify(inventoryRepository).updateQuantityLevel(id2, QuantityLevel.ENOUGH)
    }

    @Test
    fun `restock does nothing for empty list`() {
        adapter.restock(emptyList())
        // no interactions with repository
    }
}
```

- [ ] **Step 5: Run the test to confirm it fails**

```
cd apps/api && ./gradlew test --tests "*.InventoryRestockAdapterTest"
```

Expected: FAIL (class does not exist yet)

- [ ] **Step 6: Create `InventoryRestockAdapter.kt`**

```kotlin
package com.foodstock.shopping.adapter.out

import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InventoryRestockAdapter(
    private val inventoryRepository: InventoryRepository
) : RestockItemsPort {

    override fun restock(itemIds: List<UUID>) {
        itemIds.forEach { inventoryRepository.updateQuantityLevel(it, QuantityLevel.ENOUGH) }
    }
}
```

- [ ] **Step 7: Run all tests to confirm**

```
cd apps/api && ./gradlew test --tests "*.InventoryRestockAdapterTest"
```

Expected: all PASS

- [ ] **Step 8: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/out/RestockItemsPort.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/adapter/out/InventoryRestockAdapter.kt \
        apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/out/InventoryRepository.kt \
        apps/api/src/main/kotlin/com/foodstock/inventory/adapter/out/InventoryJpaRepository.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/adapter/out/InventoryRestockAdapterTest.kt
git commit -m "feat(shopping): add RestockItemsPort and InventoryRestockAdapter for completion restock"
```

---

## Task 7: Use Case Interfaces

**Files:** 6 new files in `apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/`

- [ ] **Step 1: Create all six use case files**

`StartShoppingUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class StartShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface StartShoppingUseCase {
    fun start(command: StartShoppingCommand): ShoppingList
}
```

`CompleteShoppingUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class CompleteShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface CompleteShoppingUseCase {
    fun complete(command: CompleteShoppingCommand): ShoppingList
}
```

`CancelShoppingUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingList
import java.util.UUID

data class CancelShoppingCommand(val listId: UUID, val userId: UUID, val listVersion: Long)

interface CancelShoppingUseCase {
    fun cancel(command: CancelShoppingCommand): ShoppingList
}
```

`AddShoppingItemUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

data class AddShoppingItemCommand(
    val listId: UUID,
    val userId: UUID,
    val listVersion: Long,
    val name: String,
    val quantity: Int,
    val inventoryItemId: UUID? = null
)

interface AddShoppingItemUseCase {
    fun addItem(command: AddShoppingItemCommand): ShoppingListItem
}
```

`RemoveShoppingItemUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import java.util.UUID

data class RemoveShoppingItemCommand(
    val listId: UUID,
    val itemId: UUID,
    val userId: UUID,
    val listVersion: Long
)

interface RemoveShoppingItemUseCase {
    fun removeItem(command: RemoveShoppingItemCommand)
}
```

`UpdateShoppingItemUseCase.kt`:
```kotlin
package com.foodstock.shopping.domain.port.`in`

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.util.UUID

data class UpdateShoppingItemCommand(
    val listId: UUID,
    val itemId: UUID,
    val userId: UUID,
    val listVersion: Long,
    val quantity: Int? = null,
    val checked: Boolean? = null
)

interface UpdateShoppingItemUseCase {
    fun updateItem(command: UpdateShoppingItemCommand): ShoppingListItem
}
```

- [ ] **Step 2: Confirm the project compiles**

```
cd apps/api && ./gradlew compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/port/in/
git commit -m "feat(shopping): add six lifecycle use case interfaces"
```

---

## Task 8: ShoppingListService — State Transitions (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/config/ShoppingConfig.kt`

- [ ] **Step 1: Add new mocks and failing tests to `ShoppingListServiceTest.kt`**

Replace the class header and add imports and new mocks (keep all existing tests):

```kotlin
package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import com.foodstock.shopping.domain.port.out.RunningOutItem
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import jakarta.persistence.OptimisticLockException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ShoppingListServiceTest {

    private val shoppingListRepository: ShoppingListRepository = mock()
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort = mock()
    private val memberRolePort: MemberRolePort = mock()
    private val restockItemsPort: RestockItemsPort = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = ShoppingListService(
        shoppingListRepository, runningOutItemsQueryPort, memberRolePort, restockItemsPort, fixedClock
    )

    // ... keep all existing test methods unchanged ...
```

Then append these state-transition test methods:

```kotlin
    // --- State transitions ---

    private fun aList(
        listId: UUID = UUID.randomUUID(),
        houseId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        status: ShoppingListStatus = ShoppingListStatus.OPEN,
        version: Long = 0
    ): ShoppingList {
        val now = LocalDateTime.now(fixedClock)
        return ShoppingList(id = listId, houseId = houseId, name = "Weekly", status = status,
            createdBy = userId, createdAt = now, updatedAt = now, version = version)
    }

    @Test
    fun `start transitions OPEN list to SHOPPING`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.start(StartShoppingCommand(listId, userId, listVersion = 0))

        assertEquals(ShoppingListStatus.SHOPPING, result.status)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `start throws InvalidShoppingListStateException when list is not OPEN`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `start throws ShoppingListAccessDeniedException when caller is not OWNER`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)

        assertThrows<ShoppingListAccessDeniedException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `start throws OptimisticLockException when version is stale`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, version = 2)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)

        assertThrows<OptimisticLockException> {
            service.start(StartShoppingCommand(listId, userId, listVersion = 1))
        }
    }

    @Test
    fun `complete restocks checked inventory items and marks list COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val inventoryItemId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 1)
        val now = LocalDateTime.now(fixedClock)
        val checkedItem = ShoppingListItem(UUID.randomUUID(), listId, inventoryItemId, "Milk", 1, true, now)
        val uncheckedItem = ShoppingListItem(UUID.randomUUID(), listId, null, "Manual", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(checkedItem, uncheckedItem))
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.complete(CompleteShoppingCommand(listId, userId, listVersion = 1))

        assertEquals(ShoppingListStatus.COMPLETED, result.status)
        verify(restockItemsPort).restock(listOf(inventoryItemId))
    }

    @Test
    fun `complete does not call restock when no checked inventory items exist`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(emptyList())
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        service.complete(CompleteShoppingCommand(listId, userId, listVersion = 0))

        verify(restockItemsPort, never()).restock(any())
    }

    @Test
    fun `complete throws InvalidShoppingListStateException when list is not SHOPPING`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.complete(CompleteShoppingCommand(listId, userId, listVersion = 0))
        }
    }

    @Test
    fun `cancel transitions OPEN list to CANCELLED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.cancel(CancelShoppingCommand(listId, userId, listVersion = 0))

        assertEquals(ShoppingListStatus.CANCELLED, result.status)
    }

    @Test
    fun `cancel throws InvalidShoppingListStateException when list is already COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.COMPLETED, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.OWNER)

        assertThrows<InvalidShoppingListStateException> {
            service.cancel(CancelShoppingCommand(listId, userId, listVersion = 0))
        }
    }
```

- [ ] **Step 2: Run tests to confirm they fail**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListServiceTest"
```

Expected: FAIL (service constructor mismatch + missing methods)

- [ ] **Step 3: Implement state transitions in `ShoppingListService.kt`**

```kotlin
package com.foodstock.shopping.domain.service

import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingItemNotFoundException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.exception.ShoppingListNotFoundException
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import com.foodstock.shopping.domain.port.out.RestockItemsPort
import com.foodstock.shopping.domain.port.out.RunningOutItemsQueryPort
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import jakarta.persistence.OptimisticLockException
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class ShoppingListService(
    private val shoppingListRepository: ShoppingListRepository,
    private val runningOutItemsQueryPort: RunningOutItemsQueryPort,
    private val memberRolePort: MemberRolePort,
    private val restockItemsPort: RestockItemsPort,
    private val clock: Clock
) : GenerateShoppingListUseCase, GetShoppingListsUseCase, GetShoppingListUseCase,
    StartShoppingUseCase, CompleteShoppingUseCase, CancelShoppingUseCase,
    AddShoppingItemUseCase, RemoveShoppingItemUseCase, UpdateShoppingItemUseCase {

    override fun generateFromRunningOutItems(command: GenerateShoppingListCommand): ShoppingList {
        val now = LocalDateTime.now(clock)
        val list = ShoppingList(
            id = UUID.randomUUID(), houseId = command.houseId, name = command.listName,
            status = ShoppingListStatus.OPEN, createdBy = command.createdBy,
            createdAt = now, updatedAt = now
        )
        val savedList = shoppingListRepository.save(list)
        runningOutItemsQueryPort.findRunningOutItems(command.houseId).forEach { item ->
            shoppingListRepository.saveItem(
                ShoppingListItem(UUID.randomUUID(), savedList.id, item.itemId, item.name, 1, false, now)
            )
        }
        return savedList
    }

    override fun getShoppingLists(houseId: UUID): List<ShoppingList> =
        shoppingListRepository.findAllByHouseId(houseId)

    override fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>> {
        val list = shoppingListRepository.findById(listId) ?: throw ShoppingListNotFoundException(listId)
        return Pair(list, shoppingListRepository.findItemsByListId(listId))
    }

    override fun start(command: StartShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status != ShoppingListStatus.OPEN)
            throw InvalidShoppingListStateException("Cannot start shopping: list is ${list.status}")
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.SHOPPING, updatedAt = LocalDateTime.now(clock)))
    }

    @Transactional
    override fun complete(command: CompleteShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status != ShoppingListStatus.SHOPPING)
            throw InvalidShoppingListStateException("Cannot complete: list is ${list.status}")
        val restockIds = shoppingListRepository.findItemsByListId(command.listId)
            .filter { it.checked && it.inventoryItemId != null }
            .mapNotNull { it.inventoryItemId }
        if (restockIds.isNotEmpty()) restockItemsPort.restock(restockIds)
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.COMPLETED, updatedAt = LocalDateTime.now(clock)))
    }

    override fun cancel(command: CancelShoppingCommand): ShoppingList {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireOwner(list.houseId, command.userId)
        if (list.status !in listOf(ShoppingListStatus.OPEN, ShoppingListStatus.SHOPPING))
            throw InvalidShoppingListStateException("Cannot cancel: list is ${list.status}")
        return shoppingListRepository.update(list.copy(status = ShoppingListStatus.CANCELLED, updatedAt = LocalDateTime.now(clock)))
    }

    override fun addItem(command: AddShoppingItemCommand): ShoppingListItem {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        if (command.inventoryItemId != null) {
            val duplicate = shoppingListRepository.findItemsByListId(command.listId)
                .any { it.inventoryItemId == command.inventoryItemId }
            if (duplicate) throw InvalidShoppingListStateException("Item with inventoryItemId ${command.inventoryItemId} already exists in list")
        }
        val item = ShoppingListItem(UUID.randomUUID(), command.listId, command.inventoryItemId,
            command.name, command.quantity, false, LocalDateTime.now(clock))
        val saved = shoppingListRepository.saveItem(item)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
        return saved
    }

    override fun removeItem(command: RemoveShoppingItemCommand) {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        val item = shoppingListRepository.findItemById(command.itemId)
        if (item == null || item.shoppingListId != command.listId) throw ShoppingItemNotFoundException(command.itemId)
        shoppingListRepository.deleteItem(command.itemId)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
    }

    override fun updateItem(command: UpdateShoppingItemCommand): ShoppingListItem {
        val list = shoppingListRepository.findById(command.listId) ?: throw ShoppingListNotFoundException(command.listId)
        assertVersion(list.version, command.listVersion)
        requireActiveMember(list.houseId, command.userId)
        assertMutable(list)
        val item = shoppingListRepository.findItemById(command.itemId)
        if (item == null || item.shoppingListId != command.listId) throw ShoppingItemNotFoundException(command.itemId)
        val updated = item.copy(
            quantity = command.quantity ?: item.quantity,
            checked = command.checked ?: item.checked
        )
        val saved = shoppingListRepository.updateItem(updated)
        shoppingListRepository.update(list.copy(updatedAt = LocalDateTime.now(clock)))
        return saved
    }

    private fun assertVersion(current: Long, provided: Long) {
        if (current != provided) throw OptimisticLockException("Shopping list version mismatch")
    }

    private fun requireOwner(houseId: UUID, userId: UUID) {
        if (memberRolePort.getRole(houseId, userId) != HouseRole.OWNER)
            throw ShoppingListAccessDeniedException("Only the house owner can perform this action")
    }

    private fun requireActiveMember(houseId: UUID, userId: UUID) {
        memberRolePort.getRole(houseId, userId)
            ?: throw ShoppingListAccessDeniedException("User is not an active member of this house")
    }

    private fun assertMutable(list: ShoppingList) {
        if (list.status !in listOf(ShoppingListStatus.OPEN, ShoppingListStatus.SHOPPING))
            throw InvalidShoppingListStateException("Cannot modify items on a ${list.status} list")
    }
}
```

- [ ] **Step 4: Update `ShoppingConfig.kt` to wire new dependencies**

```kotlin
package com.foodstock.shopping.config

import com.foodstock.shopping.adapter.out.InventoryRestockAdapter
import com.foodstock.shopping.adapter.out.InventoryRunningOutAdapter
import com.foodstock.shopping.adapter.out.MemberRoleAdapter
import com.foodstock.shopping.adapter.out.ShoppingListJpaRepository
import com.foodstock.shopping.domain.service.ShoppingListService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ShoppingConfig(
    private val shoppingListJpaRepository: ShoppingListJpaRepository,
    private val inventoryRunningOutAdapter: InventoryRunningOutAdapter,
    private val memberRoleAdapter: MemberRoleAdapter,
    private val inventoryRestockAdapter: InventoryRestockAdapter,
    private val clock: Clock
) {
    @Bean
    fun shoppingListService(): ShoppingListService = ShoppingListService(
        shoppingListRepository = shoppingListJpaRepository,
        runningOutItemsQueryPort = inventoryRunningOutAdapter,
        memberRolePort = memberRoleAdapter,
        restockItemsPort = inventoryRestockAdapter,
        clock = clock
    )
}
```

- [ ] **Step 5: Run state-transition tests to confirm they pass**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListServiceTest"
```

Expected: all PASS

- [ ] **Step 6: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt \
        apps/api/src/main/kotlin/com/foodstock/shopping/config/ShoppingConfig.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt
git commit -m "feat(shopping): implement state transitions in ShoppingListService (TDD)"
```

---

## Task 9: ShoppingListService — Item Mutations (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt`

> The service implementation was already written in Task 8. This task writes and verifies the item-mutation tests to confirm the implementation is correct.

- [ ] **Step 1: Append item-mutation tests to `ShoppingListServiceTest.kt`**

```kotlin
    // --- Item mutations ---

    @Test
    fun `addItem saves new item and bumps list updatedAt`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(emptyList())
        whenever(shoppingListRepository.saveItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 2))

        assertEquals("Bread", result.name)
        assertEquals(2, result.quantity)
        assertEquals(false, result.checked)
        assertEquals(listId, result.shoppingListId)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `addItem throws InvalidShoppingListStateException when list is COMPLETED`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.COMPLETED, version = 0)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)

        assertThrows<InvalidShoppingListStateException> {
            service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 1))
        }
    }

    @Test
    fun `addItem throws InvalidShoppingListStateException when inventoryItemId already in list`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val inventoryItemId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val existing = ShoppingListItem(UUID.randomUUID(), listId, inventoryItemId, "Milk", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(existing))

        assertThrows<InvalidShoppingListStateException> {
            service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Milk", quantity = 1, inventoryItemId = inventoryItemId))
        }
    }

    @Test
    fun `addItem allows duplicate names when inventoryItemId is null`() {
        val listId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val existing = ShoppingListItem(UUID.randomUUID(), listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemsByListId(listId)).thenReturn(listOf(existing))
        whenever(shoppingListRepository.saveItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.addItem(AddShoppingItemCommand(listId, userId, listVersion = 0, name = "Bread", quantity = 1))

        assertEquals("Bread", result.name)
    }

    @Test
    fun `removeItem deletes item and bumps list updatedAt`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        service.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion = 0))

        verify(shoppingListRepository).deleteItem(itemId)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `removeItem throws ShoppingItemNotFoundException when item belongs to a different list`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, UUID.randomUUID(), null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)

        assertThrows<ShoppingItemNotFoundException> {
            service.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion = 0))
        }
    }

    @Test
    fun `updateItem applies quantity and checked changes`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.SHOPPING, version = 1)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 1, false, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.updateItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.updateItem(UpdateShoppingItemCommand(listId, itemId, userId, listVersion = 1, quantity = 3, checked = true))

        assertEquals(3, result.quantity)
        assertEquals(true, result.checked)
        verify(shoppingListRepository).update(any())
    }

    @Test
    fun `updateItem preserves existing values for null fields`() {
        val listId = UUID.randomUUID()
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val list = aList(listId = listId, status = ShoppingListStatus.OPEN, version = 0)
        val now = LocalDateTime.now(fixedClock)
        val item = ShoppingListItem(itemId, listId, null, "Bread", 5, true, now)
        whenever(shoppingListRepository.findById(listId)).thenReturn(list)
        whenever(memberRolePort.getRole(list.houseId, userId)).thenReturn(HouseRole.MEMBER)
        whenever(shoppingListRepository.findItemById(itemId)).thenReturn(item)
        whenever(shoppingListRepository.updateItem(any())).thenAnswer { it.arguments[0] as ShoppingListItem }
        whenever(shoppingListRepository.update(any())).thenAnswer { it.arguments[0] as ShoppingList }

        val result = service.updateItem(UpdateShoppingItemCommand(listId, itemId, userId, listVersion = 0, quantity = null, checked = false))

        assertEquals(5, result.quantity)
        assertEquals(false, result.checked)
    }
```

Also add the missing import at the top:
```kotlin
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
```

- [ ] **Step 2: Run item-mutation tests**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListServiceTest"
```

Expected: all PASS

- [ ] **Step 3: Commit**

```
git add apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt
git commit -m "test(shopping): add item mutation tests for ShoppingListService"
```

---

## Task 10: Controller — State Transitions (TDD)

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/AddItemRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/UpdateItemRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/ShoppingListItemResponse.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt`

> DTOs for item mutations are created here (before the controller) so that compilation succeeds when the full controller is written in Step 4.

- [ ] **Step 1: Create the three item DTO files**

`AddItemRequest.kt`:
```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AddItemRequest(
    @field:NotBlank val name: String,
    @field:Min(1) val quantity: Int,
    val inventoryItemId: UUID? = null
)
```

`UpdateItemRequest.kt`:
```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import jakarta.validation.constraints.Min

data class UpdateItemRequest(
    @field:Min(1) val quantity: Int? = null,
    val checked: Boolean? = null
)
```

`ShoppingListItemResponse.kt`:
```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import com.foodstock.shopping.domain.model.ShoppingListItem
import java.time.LocalDateTime
import java.util.UUID

data class ShoppingListItemResponse(
    val id: UUID,
    val shoppingListId: UUID,
    val name: String,
    val quantity: Int,
    val checked: Boolean,
    val inventoryItemId: UUID?,
    val createdAt: LocalDateTime
)

fun ShoppingListItem.toItemResponse() = ShoppingListItemResponse(
    id = id, shoppingListId = shoppingListId, name = name, quantity = quantity,
    checked = checked, inventoryItemId = inventoryItemId, createdAt = createdAt
)
```

- [ ] **Step 2: Add `@MockBean` declarations and failing tests to `ShoppingListControllerTest.kt`**

Add these imports to the existing test file:
```kotlin
import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import com.foodstock.shopping.domain.exception.ShoppingListAccessDeniedException
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import org.mockito.kotlin.verify
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
```

Add these `@MockBean` fields to the test class (all 6 new use cases declared upfront so the controller context loads):
```kotlin
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
```

Append these test methods:

```kotlin
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
```

- [ ] **Step 2: Run to confirm tests fail**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListControllerTest"
```

Expected: FAIL (endpoints not wired)

- [ ] **Step 3: Add state-transition endpoints to `ShoppingListController.kt`**

```kotlin
package com.foodstock.shopping.adapter.`in`

import com.foodstock.shopping.adapter.`in`.dto.AddItemRequest
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListDetailResponse
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListItemResponse
import com.foodstock.shopping.adapter.`in`.dto.ShoppingListResponse
import com.foodstock.shopping.adapter.`in`.dto.UpdateItemRequest
import com.foodstock.shopping.adapter.`in`.dto.toDetailResponse
import com.foodstock.shopping.adapter.`in`.dto.toItemResponse
import com.foodstock.shopping.adapter.`in`.dto.toResponse
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.AddShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.CancelShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CancelShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingCommand
import com.foodstock.shopping.domain.port.`in`.CompleteShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListCommand
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListUseCase
import com.foodstock.shopping.domain.port.`in`.GetShoppingListsUseCase
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.RemoveShoppingItemUseCase
import com.foodstock.shopping.domain.port.`in`.StartShoppingCommand
import com.foodstock.shopping.domain.port.`in`.StartShoppingUseCase
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemCommand
import com.foodstock.shopping.domain.port.`in`.UpdateShoppingItemUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@RequestMapping("/api/v1/shopping-lists")
class ShoppingListController(
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val getShoppingListsUseCase: GetShoppingListsUseCase,
    private val getShoppingListUseCase: GetShoppingListUseCase,
    private val startShoppingUseCase: StartShoppingUseCase,
    private val completeShoppingUseCase: CompleteShoppingUseCase,
    private val cancelShoppingUseCase: CancelShoppingUseCase,
    private val addShoppingItemUseCase: AddShoppingItemUseCase,
    private val removeShoppingItemUseCase: RemoveShoppingItemUseCase,
    private val updateShoppingItemUseCase: UpdateShoppingItemUseCase
) {

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateList(
        @RequestBody request: GenerateShoppingListRequest,
        @RequestHeader("X-User-Id") userId: UUID
    ): ShoppingListResponse =
        generateShoppingListUseCase.generateFromRunningOutItems(
            GenerateShoppingListCommand(request.houseId, userId, request.listName)
        ).toResponse()

    @GetMapping
    fun getShoppingLists(@RequestHeader("X-House-Id") houseId: UUID): List<ShoppingListResponse> =
        getShoppingListsUseCase.getShoppingLists(houseId).map { it.toResponse() }

    @GetMapping("/{listId}")
    fun getShoppingList(@PathVariable listId: UUID): ShoppingListDetailResponse =
        getShoppingListUseCase.getShoppingList(listId).toDetailResponse()

    @PostMapping("/{listId}/start")
    fun startShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        startShoppingUseCase.start(StartShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/complete")
    fun completeShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        completeShoppingUseCase.complete(CompleteShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/cancel")
    fun cancelShopping(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ): ShoppingListResponse =
        cancelShoppingUseCase.cancel(CancelShoppingCommand(listId, userId, listVersion)).toResponse()

    @PostMapping("/{listId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @PathVariable listId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long,
        @RequestBody @Valid request: AddItemRequest
    ): ShoppingListItemResponse =
        addShoppingItemUseCase.addItem(
            AddShoppingItemCommand(listId, userId, listVersion, request.name, request.quantity, request.inventoryItemId)
        ).toItemResponse()

    @DeleteMapping("/{listId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeItem(
        @PathVariable listId: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long
    ) = removeShoppingItemUseCase.removeItem(RemoveShoppingItemCommand(listId, itemId, userId, listVersion))

    @PatchMapping("/{listId}/items/{itemId}")
    fun updateItem(
        @PathVariable listId: UUID,
        @PathVariable itemId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestHeader("X-List-Version") listVersion: Long,
        @RequestBody request: UpdateItemRequest
    ): ShoppingListItemResponse {
        if (request.quantity == null && request.checked == null)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one of quantity or checked must be provided")
        return updateShoppingItemUseCase.updateItem(
            UpdateShoppingItemCommand(listId, itemId, userId, listVersion, request.quantity, request.checked)
        ).toItemResponse()
    }
}
```

Also update `ShoppingConfig` to wire all new use case beans (the service already implements all interfaces, so no extra beans needed — Spring resolves them from the `shoppingListService()` bean by type):

> No `ShoppingConfig` change needed: Spring resolves each use case interface (`StartShoppingUseCase`, etc.) from the `shoppingListService()` bean automatically because `ShoppingListService` implements them all.

- [ ] **Step 4: Run state-transition controller tests**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListControllerTest"
```

Expected: state-transition tests PASS (item tests fail — that's next task)

- [ ] **Step 5: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt \
        apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt
git commit -m "feat(shopping): add state-transition endpoints to ShoppingListController (TDD)"
```

---

## Task 11: Controller — Item Mutation Tests (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`

> DTOs and controller endpoints were already written in Task 10. This task confirms item mutation behaviour with targeted tests.

- [ ] **Step 1: Add item mutation tests to `ShoppingListControllerTest.kt`**

Add import (if not already present from Task 10):
```kotlin
import com.foodstock.shopping.domain.exception.ShoppingItemNotFoundException
```

Append these test methods:

```kotlin
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
```

- [ ] **Step 2: Run item mutation tests to confirm they pass**

```
cd apps/api && ./gradlew test --tests "*.ShoppingListControllerTest"
```

Expected: all PASS

- [ ] **Step 3: Run the full test suite**

```
cd apps/api && ./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 4: Commit**

```
git add apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt
git commit -m "test(shopping): add item mutation controller tests"
```
