# Shopping List Lifecycle — Design Spec

**Date:** 2026-06-07  
**Status:** Approved

## Overview

Implement the full lifecycle of a shopping list in the `shopping` bounded context.  
Currently the domain model already defines `OPEN`, `SHOPPING`, and `COMPLETED` statuses, but no endpoints exist to transition state or mutate items after generation.  
This feature adds state-transition endpoints, item management endpoints, and an inventory restock side-effect on completion.

---

## State Machine

```
OPEN ──────► SHOPPING ──────► COMPLETED
  │               │
  └───────────────┴──────────► CANCELLED
```

| Transition | Guard |
|---|---|
| `OPEN → SHOPPING` | OWNER or ADMIN |
| `SHOPPING → COMPLETED` | OWNER or ADMIN; triggers inventory restock |
| `OPEN → CANCELLED` | OWNER or ADMIN |
| `SHOPPING → CANCELLED` | OWNER or ADMIN |

Any other transition throws `InvalidShoppingListStateException` → `409 Conflict`.

`CANCELLED` is a soft-cancel: the list is preserved in history. The existing `GET /shopping-lists` endpoint is updated to exclude `CANCELLED` lists by default (returns only `OPEN`, `SHOPPING`, `COMPLETED`).

---

## API Surface

All endpoints require `X-User-Id` header for identity (existing convention).

### State Transitions (OWNER/ADMIN only)

```
POST /api/v1/shopping-lists/{listId}/start      → 200 ShoppingListResponse
POST /api/v1/shopping-lists/{listId}/complete   → 200 ShoppingListResponse
POST /api/v1/shopping-lists/{listId}/cancel     → 200 ShoppingListResponse
```

### Item Mutations (any active house member)

```
POST   /api/v1/shopping-lists/{listId}/items             → 201 ShoppingListItemResponse
DELETE /api/v1/shopping-lists/{listId}/items/{itemId}    → 204
PATCH  /api/v1/shopping-lists/{listId}/items/{itemId}    → 200 ShoppingListItemResponse
```

`PATCH` body accepts `quantity` (Int, optional) and `checked` (Boolean, optional).  
Item mutations on a `COMPLETED` or `CANCELLED` list return `409 Conflict`.

---

## Domain Changes

### `ShoppingListStatus` enum

Add `CANCELLED`. Stored as `VARCHAR(50)` in the DB — no Flyway migration needed.

### `ShoppingListRepository` — new methods

```kotlin
fun update(list: ShoppingList): ShoppingList
fun findItemById(itemId: UUID): ShoppingListItem?
fun updateItem(item: ShoppingListItem): ShoppingListItem
fun deleteItem(itemId: UUID)
```

---

## Ports

### Inbound (port/in) — Use Case Interfaces

**State transitions:**

```kotlin
// StartShoppingUseCase.kt
data class StartShoppingCommand(val listId: UUID, val userId: UUID)
interface StartShoppingUseCase {
    fun start(command: StartShoppingCommand): ShoppingList
}

// CompleteShoppingUseCase.kt
data class CompleteShoppingCommand(val listId: UUID, val userId: UUID)
interface CompleteShoppingUseCase {
    fun complete(command: CompleteShoppingCommand): ShoppingList
}

// CancelShoppingUseCase.kt
data class CancelShoppingCommand(val listId: UUID, val userId: UUID)
interface CancelShoppingUseCase {
    fun cancel(command: CancelShoppingCommand): ShoppingList
}
```

**Item mutations:**

```kotlin
// AddShoppingItemUseCase.kt
data class AddShoppingItemCommand(
    val listId: UUID, val userId: UUID,
    val name: String, val quantity: Int,
    val inventoryItemId: UUID? = null
)
interface AddShoppingItemUseCase {
    fun addItem(command: AddShoppingItemCommand): ShoppingListItem
}

// RemoveShoppingItemUseCase.kt
data class RemoveShoppingItemCommand(val listId: UUID, val itemId: UUID, val userId: UUID)
interface RemoveShoppingItemUseCase {
    fun removeItem(command: RemoveShoppingItemCommand)
}

// UpdateShoppingItemUseCase.kt
data class UpdateShoppingItemCommand(
    val listId: UUID, val itemId: UUID, val userId: UUID,
    val quantity: Int? = null, val checked: Boolean? = null
)
interface UpdateShoppingItemUseCase {
    fun updateItem(command: UpdateShoppingItemCommand): ShoppingListItem
}
```

### Outbound (port/out) — Anti-Corruption Ports

**`MemberRolePort`** — shopping domain's view of house membership:

```kotlin
enum class HouseRole { OWNER, ADMIN, MEMBER }

interface MemberRolePort {
    fun getRole(houseId: UUID, userId: UUID): HouseRole?  // null = not a member
}
```

**`RestockItemsPort`** — shopping domain's view of inventory restock:

```kotlin
interface RestockItemsPort {
    fun restock(itemIds: List<UUID>)
}
```

---

## Service Logic (`ShoppingListService`)

Implements all six new use cases in addition to existing ones.

### Authorization helpers (private)

```kotlin
private fun requireAdminOrOwner(houseId: UUID, userId: UUID)
// calls memberRolePort.getRole; throws UnauthorizedMemberOperationException if null/MEMBER

private fun requireActiveMember(houseId: UUID, userId: UUID)
// calls memberRolePort.getRole; throws UnauthorizedMemberOperationException if null
```

### State Transition Logic

**`startShopping`:**
1. Load list — throw `ShoppingListNotFoundException` if absent
2. `requireAdminOrOwner(list.houseId, userId)`
3. Assert `list.status == OPEN` — throw `InvalidShoppingListStateException` otherwise
4. Save list with `status = SHOPPING`, `updatedAt = now`

**`completeShopping`:**
1. Load list — throw `ShoppingListNotFoundException` if absent
2. `requireAdminOrOwner(list.houseId, userId)`
3. Assert `list.status == SHOPPING` — throw `InvalidShoppingListStateException` otherwise
4. Load items; filter `checked && inventoryItemId != null`
5. If filtered list non-empty: `restockItemsPort.restock(ids)`
6. Save list with `status = COMPLETED`, `updatedAt = now`

**`cancelShopping`:**
1. Load list — throw `ShoppingListNotFoundException` if absent
2. `requireAdminOrOwner(list.houseId, userId)`
3. Assert `list.status in [OPEN, SHOPPING]` — throw `InvalidShoppingListStateException` otherwise
4. Save list with `status = CANCELLED`, `updatedAt = now`

### Item Mutation Logic

All item mutations share: load list → `requireActiveMember` → assert status is `OPEN` or `SHOPPING`.

**`addItem`:** Create `ShoppingListItem` with new UUID, save via `shoppingListRepository.saveItem`.

**`removeItem`:** Load item via `findItemById` — throw `ShoppingItemNotFoundException` (404) if absent or item's `shoppingListId != listId`. Delete via `deleteItem`.

**`updateItem`:** Load item; apply non-null fields from command; save via `updateItem`.

---

## Adapters (shopping/adapter/out)

### `MemberRoleAdapter`

Implements `MemberRolePort`. Bridges to `HouseMemberRepository` (household domain).  
Maps `HouseMember.role` → `HouseRole`; returns `null` if member not found or status is not `ACTIVE`.

### `InventoryRestockAdapter` (extended)

Implements `RestockItemsPort`. Calls `inventoryRepository.updateQuantityLevel(itemId, QuantityLevel.ENOUGH)` for each ID.  
`InventoryRepository` gains one new method: `updateQuantityLevel(itemId: UUID, level: QuantityLevel)`.  
Implemented in `InventoryItemJpaRepository` via a `@Modifying @Query`.

---

## Exception Handling

| Exception | HTTP Status |
|---|---|
| `ShoppingListNotFoundException` | 404 |
| `ShoppingItemNotFoundException` | 404 |
| `InvalidShoppingListStateException` | 409 |
| `UnauthorizedMemberOperationException` | 403 |

`InvalidShoppingListStateException` is new and must be registered in `GlobalExceptionHandler`.

---

## Testing Strategy

- **`ShoppingListServiceTest`** — unit tests for all six new use cases; mock `memberRolePort`, `restockItemsPort`, `shoppingListRepository`
- **`ShoppingListControllerTest`** — `@WebMvcTest` for all six new endpoints; mock use case beans
- **`MemberRoleAdapter`** — unit test verifying `ACTIVE` member maps correctly, non-member returns null
- **`InventoryRestockAdapter`** — unit test verifying `restock` calls `updateQuantityLevel` per item

Coverage target: ≥ 80% on all new lines (CI gate requirement).

---

## Out of Scope

- JWT claim extraction (existing TODO; `X-User-Id` header convention used throughout)
- Push notifications or events on state transition
- Shopping list archival or pagination of cancelled lists
