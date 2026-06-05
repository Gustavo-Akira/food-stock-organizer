# DTO Extraction — Design Spec

**Date:** 2026-06-05
**Branch:** `refactor/extract-inbound-dtos`

## Goal

Extract all request and response DTO data classes out of their `@RestController` files into a dedicated `dto/` sub-package under each bounded context's `adapter/in/` layer. Verify 80% line coverage on all modified/added lines.

## File Structure

Each bounded context gains a `dto/` sub-package:

```
auth/adapter/in/
  AuthController.kt
  dto/
    RegisterRequest.kt
    LoginRequest.kt
    UserResponse.kt
    LoginResponse.kt

household/adapter/in/
  HouseController.kt
  dto/
    CreateHouseRequest.kt
    InviteMemberRequest.kt

inventory/adapter/in/
  InventoryController.kt
  dto/
    AddItemRequest.kt
    UpdateQuantityRequest.kt
    InventoryItemResponse.kt   ← toResponse() extension moves here

shopping/adapter/in/
  ShoppingListController.kt
  dto/
    GenerateShoppingListRequest.kt
```

### Mapping extension

`InventoryController` has a private `InventoryItem.toResponse()` extension function. It moves to `InventoryItemResponse.kt` as a top-level (non-private) extension so it is visible from the controller across the package boundary.

## Out of Scope

- `HouseController` and `ShoppingListController` return domain models (`House`, `HouseMember`, `ShoppingList`) directly. This pre-existing leakage is a known TODO and is **not** addressed in this PR.
- No changes to domain models, ports, services, or JPA adapters.

## Coverage Strategy

One `@WebMvcTest` class per controller in `src/test/.../adapter/in/`:

| Test class | Scenarios |
|---|---|
| `AuthControllerTest` | POST /register → 201; POST /login → 200; invalid body → 400 |
| `HouseControllerTest` | POST /houses → 201; POST /houses/{id}/members → 201; missing X-User-Id header → 400 |
| `InventoryControllerTest` | POST /inventory → 201; PATCH /inventory/{id}/quantity → 200; blank name → 400; null quantityLevel → 400 |
| `ShoppingListControllerTest` | POST /shopping-lists/generate → 201; missing X-User-Id header → 400 |

- Use cases are mocked with `@MockitoBean`.
- Validation rejection tests (400) cover the `@field:NotBlank` / `@field:NotNull` annotation lines on `AddItemRequest`.
- DTO files are covered by instantiation inside controller tests — no separate DTO-only tests needed.

## Architecture Constraints (from CLAUDE.md)

- Domain layer must not gain any infrastructure imports from this refactor.
- No `@Service` or `@Component` on domain services.
- `Clock` injection pattern unchanged.
- All schema and wiring changes must remain in `adapter/` or `config/` layers.
