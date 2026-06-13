# JWT Authentication Principal — Design Spec

**Date:** 2026-06-13
**Scope:** Wire Spring Security JWT filter; replace raw `X-User-Id` / `X-House-Id` header stubs with `@AuthenticationPrincipal`; add house membership verification in the inventory context.

---

## Problem

`SecurityConfig` enforces `anyRequest().authenticated()` but no JWT filter is registered in the filter chain, so Bearer tokens are never validated. Controllers trust raw `X-User-Id` and `X-House-Id` request headers for identity and house context — any caller can spoof them. House ownership is not verified in any inventory operation.

---

## Goals

1. Wire a `JwtAuthenticationFilter` that validates Bearer tokens and populates `SecurityContextHolder`.
2. Replace all `@RequestHeader("X-User-Id")` stubs in `HouseController` with `@AuthenticationPrincipal`.
3. Replace the identity stubs in `InventoryController` with `@AuthenticationPrincipal`; keep `X-House-Id` header for house context.
4. Verify that the authenticated user is an active member of the requested house before every inventory operation.

---

## Architecture

### 1. JWT Filter & Principal

**Location:** `auth/adapter/in/`

**`AuthenticatedUser`** — data class holding `userId: UUID` and `email: String`. Used as the Spring Security principal and injected via `@AuthenticationPrincipal`.

**`JwtPort`** (existing) — add one method:
```kotlin
fun extractUserId(token: String): UUID
```

**`JwtAdapter`** (existing) — implement `extractUserId` by reading the `userId` claim from the parsed JWT payload and converting it to `UUID`.

**`JwtAuthenticationFilter : OncePerRequestFilter`** — reads `Authorization: Bearer <token>`, calls `jwtPort.validateToken()`, on success extracts `userId` + `email` into `AuthenticatedUser`, wraps it in `UsernamePasswordAuthenticationToken`, and sets it in `SecurityContextHolder`. On missing or invalid token it clears the context and lets the request fall through (Spring Security then returns 401).

**`SecurityConfig`** — add:
```kotlin
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
```

### 2. House Membership Verification Port (Inventory)

Follows the identical pattern used by the `shopping` context (`MemberRolePort` + `MemberRoleAdapter`).

**`HouseMemberCheckPort`** — new interface in `inventory/domain/port/out/`:
```kotlin
interface HouseMemberCheckPort {
    fun isActiveMember(houseId: UUID, userId: UUID): Boolean
}
```

**`HouseMemberCheckAdapter`** — in `inventory/adapter/out/`, `@Component`, queries `HouseMemberRepository` (from the `household` context). Returns `true` only when the membership record exists and `status == ACTIVE`.

**`HouseAccessDeniedException`** — new domain exception in `inventory/domain/exception/`, maps to HTTP 403 via `GlobalExceptionHandler`.

**`InventoryService`** — receives `houseMemberCheckPort: HouseMemberCheckPort` as a constructor parameter (wired in `InventoryConfig`). Before each operation it resolves `houseId` (from the command or from the looked-up item) and calls `isActiveMember(houseId, userId)`, throwing `HouseAccessDeniedException` on failure.

**Command changes:**
- `AddItemCommand` gains `userId: UUID`.
- `GetInventoryUseCase.getInventory` signature gains `userId: UUID`.
- `UpdateItemQuantityCommand` gains `userId: UUID`.
- `GetInventoryItemUseCase.getInventoryItem` gains `userId: UUID`.

### 3. Controller Changes

**`InventoryController`** — all four endpoints receive `@AuthenticationPrincipal user: AuthenticatedUser`. `houseId` continues to come from `@RequestHeader("X-House-Id")` on the two endpoints that need it (`addItem`, `getInventory`). `user.userId` is threaded into commands.

**`HouseController`** — all five `@RequestHeader("X-User-Id") userId: UUID` parameters replaced with `@AuthenticationPrincipal user: AuthenticatedUser`; `user.userId` threaded into commands. No domain logic changes.

---

## Data Flow (addItem example)

```
Client → POST /api/v1/inventory
         Authorization: Bearer <jwt>
         X-House-Id: <houseId>

JwtAuthenticationFilter
  → validateToken ✓
  → extractUserId → AuthenticatedUser(userId, email)
  → SecurityContextHolder.set(auth)

InventoryController.addItem
  → @AuthenticationPrincipal user      (userId from JWT)
  → @RequestHeader("X-House-Id") houseId

  → AddItemCommand(houseId, userId, name, ...)

InventoryService.addItem
  → houseMemberCheckPort.isActiveMember(houseId, userId)
     ✗ → throw HouseAccessDeniedException → 403
     ✓ → save item → return InventoryItemResponse
```

---

## Error Handling

| Condition | HTTP |
|-----------|------|
| Missing / invalid JWT | 401 |
| Valid JWT, user not active member of house | 403 |
| Item not found | 404 |

`HouseAccessDeniedException` is registered in `GlobalExceptionHandler` alongside existing exceptions.

---

## Testing

- `JwtAuthenticationFilter` — unit test: valid token sets principal; missing/invalid token clears context.
- `InventoryControllerTest` — existing tests keep `@AutoConfigureMockMvc(addFilters = false)`; add new test: principal injected correctly (mock `@AuthenticationPrincipal`).
- `InventoryServiceTest` — add cases: member check passes → operation proceeds; member check fails → `HouseAccessDeniedException`.
- `HouseControllerTest` — update existing tests to inject principal instead of header.
- CI diff-coverage gate: all new lines must be covered at ≥ 80%.

---

## Files Changed

| File | Change |
|------|--------|
| `auth/domain/port/out/AuthPorts.kt` | Add `extractUserId` to `JwtPort` |
| `auth/adapter/out/JwtAdapter.kt` | Implement `extractUserId` |
| `auth/adapter/in/AuthenticatedUser.kt` | New — principal data class |
| `auth/adapter/in/JwtAuthenticationFilter.kt` | New — `OncePerRequestFilter` |
| `auth/config/SecurityConfig.kt` | Register JWT filter |
| `inventory/domain/port/out/HouseMemberCheckPort.kt` | New — membership port |
| `inventory/domain/exception/HouseAccessDeniedException.kt` | New — 403 exception |
| `inventory/adapter/out/HouseMemberCheckAdapter.kt` | New — adapter |
| `inventory/domain/port/in/AddItemUseCase.kt` | Add `userId` to command |
| `inventory/domain/port/in/GetInventoryUseCase.kt` | Add `userId` param |
| `inventory/domain/port/in/UpdateItemQuantityUseCase.kt` | Add `userId` to command |
| `inventory/domain/port/in/GetInventoryItemUseCase.kt` | Add `userId` param |
| `inventory/domain/service/InventoryService.kt` | Accept port; call membership check |
| `inventory/config/InventoryConfig.kt` | Wire new adapter |
| `inventory/adapter/in/InventoryController.kt` | `@AuthenticationPrincipal`; thread userId |
| `household/adapter/in/HouseController.kt` | Replace `X-User-Id` with `@AuthenticationPrincipal` |
| `common/GlobalExceptionHandler.kt` | Handle `HouseAccessDeniedException` |
| Tests (various) | Update / add per section above |
