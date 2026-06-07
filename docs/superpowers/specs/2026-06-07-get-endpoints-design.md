# GET Endpoints — Design Spec

**Date:** 2026-06-07  
**Status:** Approved  
**Scope:** Three parallel PRs adding read (GET) endpoints across the household, inventory, and shopping bounded contexts.

---

## Background

The API currently exposes only mutation endpoints (POST/PATCH). No GET endpoints exist for any resource. The JPA repositories already implement the query methods needed (`findById`, `findAllByOwnerId`, `findAllByHouseId`, `findAllByHouseIdAndQuantityLevel`, `findItemsByListId`). What is missing is the full hexagonal layer: query use case interfaces in `domain/port/in`, implementations in the domain service, and controller mappings.

---

## Architecture

Each PR follows the established hexagonal pattern:

1. New query use case interface(s) in `domain/port/in/`
2. New query method(s) in the domain service (no Spring annotations — wired via `{Context}Config.kt`)
3. `@GetMapping` endpoint(s) in the existing `@RestController`
4. Unit tests for the service (query paths) and controller (`@WebMvcTest` slices)

Identity is provided via request headers following the existing convention (`X-User-Id` for household and shopping, `X-House-Id` for inventory). No changes to the auth layer.

---

## PR 1 — `feat/get-household`

### Endpoints

| Method | Path | Header | HTTP |
|--------|------|--------|------|
| GET | `/api/v1/houses` | `X-User-Id` | 200 |
| GET | `/api/v1/houses/{houseId}` | `X-User-Id` | 200 / 404 / 403 |
| GET | `/api/v1/houses/{houseId}/members` | `X-User-Id` | 200 / 404 / 403 |

### Authorization

- `GET /houses` — returns only houses where the caller is owner (`findAllByOwnerId`). No 403 possible; empty list if none.
- `GET /houses/{houseId}` — caller must be owner or have an `ACTIVE` membership. Throws `UnauthorizedMemberOperationException` (→ 403) if not. Throws `HouseNotFoundException` (→ 404) if the house does not exist.
- `GET /houses/{houseId}/members` — same authorization check as above before returning member list.

### New Use Cases

```
GetMyHousesUseCase       port/in  — fun getMyHouses(userId: UUID): List<House>
GetHouseUseCase          port/in  — fun getHouse(houseId: UUID, requestingUserId: UUID): House
GetHouseMembersUseCase   port/in  — fun getHouseMembers(houseId: UUID, requestingUserId: UUID): List<HouseMember>
```

### Response DTOs

`HouseResponse` and `HouseMemberResponse` already exist in `HouseController.kt` — reuse them.

---

## PR 2 — `feat/get-inventory`

### Endpoints

| Method | Path | Header | Query Param | HTTP |
|--------|------|--------|-------------|------|
| GET | `/api/v1/inventory` | `X-House-Id` | `?quantityLevel=` (optional) | 200 |
| GET | `/api/v1/inventory/{itemId}` | — | — | 200 / 404 |

### Filtering

`quantityLevel` is an optional `QuantityLevel?` enum param. When present, delegates to `findAllByHouseIdAndQuantityLevel`. When absent, delegates to `findAllByHouseId`. Invalid enum values return 400 via Spring's default binding error handling.

### New Use Cases

```
GetInventoryUseCase      port/in  — fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?): List<InventoryItem>
GetInventoryItemUseCase  port/in  — fun getInventoryItem(itemId: UUID): InventoryItem
```

`GetInventoryItemUseCase` throws `ResourceNotFoundException` if the item is not found (→ 404).

### Response DTOs

`InventoryItemResponse` and `toResponse()` already exist in the inventory DTO package — reuse them.

---

## PR 3 — `feat/get-shopping`

### Endpoints

| Method | Path | Header | HTTP |
|--------|------|--------|------|
| GET | `/api/v1/shopping-lists` | `X-House-Id` | 200 |
| GET | `/api/v1/shopping-lists/{listId}` | — | 200 / 404 |

### New Use Cases

```
GetShoppingListsUseCase  port/in  — fun getShoppingLists(houseId: UUID): List<ShoppingList>
GetShoppingListUseCase   port/in  — fun getShoppingList(listId: UUID): Pair<ShoppingList, List<ShoppingListItem>>
```

`GetShoppingListUseCase` throws `ResourceNotFoundException` if the list is not found (→ 404).

### Response DTOs

`GET /shopping-lists` reuses the existing `ShoppingListResponse`.

`GET /shopping-lists/{listId}` returns a new `ShoppingListDetailResponse`:

```json
{
  "id": "uuid",
  "houseId": "uuid",
  "name": "Weekly shop",
  "status": "OPEN",
  "createdBy": "uuid",
  "createdAt": "2026-06-07T10:00:00",
  "updatedAt": "2026-06-07T10:00:00",
  "items": [
    {
      "id": "uuid",
      "name": "Milk",
      "quantity": 2,
      "checked": false,
      "inventoryItemId": "uuid-or-null",
      "createdAt": "2026-06-07T10:00:00"
    }
  ]
}
```

---

## Error Handling

All three PRs reuse existing `GlobalExceptionHandler` mappings:

| Exception | HTTP |
|-----------|------|
| `HouseNotFoundException` / `ResourceNotFoundException` | 404 |
| `UnauthorizedMemberOperationException` | 403 |

No new exception types needed.

---

## Testing Strategy

Each PR must meet the 80% modified-line coverage gate enforced by CI.

**Service tests** — cover:
- Happy path (returns expected domain objects)
- Not-found path (exception thrown)
- Authorization failure path (household only)
- Filter variations (inventory only)

**Controller tests** (`@WebMvcTest`) — cover:
- 200 with mocked use case response
- 404 when use case throws not-found exception
- 403 when use case throws unauthorized exception (household only)
- 400 for invalid enum binding (inventory `quantityLevel` filter)

---

## Delivery Plan

Three branches, three worktrees, implemented in parallel:

| Branch | Worktree | Bounded Context |
|--------|----------|-----------------|
| `feat/get-household` | isolated | household |
| `feat/get-inventory` | isolated | inventory |
| `feat/get-shopping` | isolated | shopping |

Each branch targets `main`. PRs are independent — no shared files, no merge ordering constraint.
