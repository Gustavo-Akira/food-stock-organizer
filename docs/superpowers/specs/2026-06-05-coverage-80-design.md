# Design: Raise API Test Coverage to 80%

**Date:** 2026-06-05  
**Branch:** `chore/coverage-80`  
**Scope:** `apps/api` only (Kotlin / Spring Boot)

---

## Context

The API has two existing test files covering `HouseService` (5 tests) and `InventoryService` (4 tests). All other code — including `AuthService`, `ShoppingListService`, and all six JPA entity mappers — has zero coverage. The JaCoCo overall threshold is intentionally set to `0.00` while the project matures. This spec raises it to `0.80`.

---

## Production Code Fix

`ShoppingListService` has two hexagonal architecture violations that must be fixed before it can be properly tested:

1. **`@Service` annotation** — domain services must not have Spring annotations; they are wired via `@Configuration` classes (same pattern as `HouseService`, `InventoryService`, `AuthService`).
2. **`LocalDateTime.now()` direct call** — non-deterministic and untestable; must be replaced with an injected `Clock` parameter (same fix applied to `InventoryService` previously).

### Changes

**Modify** `apps/api/src/main/kotlin/com/foodstock/shopping/domain/service/ShoppingListService.kt`:
- Remove `@Service` and `import org.springframework.stereotype.Service`
- Add `clock: Clock` constructor parameter
- Replace both `LocalDateTime.now()` calls with `LocalDateTime.now(clock)`

**Create** `apps/api/src/main/kotlin/com/foodstock/shopping/config/ShoppingConfig.kt`:
- `@Configuration` class following the `InventoryConfig` pattern exactly
- Injects `ShoppingListJpaRepository`, `InventoryRunningOutAdapter`, and the shared `Clock` bean
- Exposes a single `@Bean fun shoppingListService(): ShoppingListService`

---

## New Test Files

All tests are pure JUnit 5 + Mockito unit tests (`@ExtendWith(MockitoExtension::class)`). No Spring context, no database.

### Domain Service Tests

**`apps/api/src/test/kotlin/com/foodstock/auth/domain/service/AuthServiceTest.kt`**

| Test | Description |
|---|---|
| `register saves user with hashed password` | Mocks `existsByEmail` → false, `hash` → hashed string, `save` → passthrough; asserts returned user fields |
| `register throws when email already in use` | Mocks `existsByEmail` → true; asserts `IllegalArgumentException` |
| `login returns token and user on success` | Mocks `findByEmail` → user, `matches` → true, `generateToken` → token string; asserts `LoginResult` fields |
| `login throws when user not found` | Mocks `findByEmail` → null; asserts `IllegalArgumentException("Invalid credentials")` |
| `login throws when password does not match` | Mocks `findByEmail` → user, `matches` → false; asserts `IllegalArgumentException("Invalid credentials")` |

**`apps/api/src/test/kotlin/com/foodstock/shopping/domain/service/ShoppingListServiceTest.kt`**

Uses a fixed `Clock` (same pattern as `InventoryServiceTest`).

| Test | Description |
|---|---|
| `generateFromRunningOutItems saves list and one item per running-out item` | Mocks two running-out items; asserts `shoppingListRepository.save` called once, `saveItem` called twice with correct fields and fixed timestamp |
| `generateFromRunningOutItems with empty running-out list saves list and no items` | Mocks empty running-out list; asserts `save` called once, `saveItem` never called; returns saved list |

### JPA Entity Mapping Tests

Each file tests `toDomain()` and `fromDomain()` via plain instantiation — no Spring, no mocks.

| File | Edge cases covered |
|---|---|
| `auth/adapter/out/UserJpaEntityTest.kt` | All four fields round-trip via both directions |
| `household/adapter/out/HouseJpaEntityTest.kt` | All five fields round-trip |
| `household/adapter/out/HouseMemberJpaEntityTest.kt` | `MemberRole` and `MemberStatus` enums preserved in both directions |
| `inventory/adapter/out/InventoryItemJpaEntityTest.kt` | Nullable `expiryDate` and `notes`: one test with values, one test with nulls |
| `shopping/adapter/out/ShoppingListJpaEntityTest.kt` | `ShoppingListStatus` enum round-trips |
| `shopping/adapter/out/ShoppingListItemJpaEntityTest.kt` | Nullable `inventoryItemId`: one test with value, one test with null |

---

## JaCoCo Threshold

In `apps/api/build.gradle.kts`, change the `jacocoTestCoverageVerification` limit:

```
minimum = "0.80".toBigDecimal()
```

The existing comment explaining the threshold strategy should be updated to reflect the new value.

---

## Coverage Estimate

| Layer | Status after changes |
|---|---|
| Domain services (4) | Fully covered |
| JPA entity mappers (6 × toDomain + fromDomain) | Fully covered |
| Domain models (data classes) | Indirectly covered via service tests |
| Controllers (4) | Not covered — Spring plumbing |
| Infrastructure adapters (JwtAdapter, BcryptPasswordHashAdapter, InventoryRunningOutAdapter) | Not covered — Spring plumbing |
| Config classes | Not covered — Spring wiring |

Expected overall instruction coverage: **~80–85%**.

---

## Out of Scope

- Controller-layer tests (`@WebMvcTest` / `@SpringBootTest`)
- Integration tests requiring PostgreSQL
- Frontend (`apps/web`, `apps/mobile`) — no CI coverage gate exists for these
