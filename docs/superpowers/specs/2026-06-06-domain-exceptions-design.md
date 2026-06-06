# Domain Exceptions Design — Issue #20

**Date:** 2026-06-06  
**Issue:** [#20 — Replace generic Java exceptions with custom domain exceptions](https://github.com/Gustavo-Akira/food-stock-organizer/issues/20)

## Problem

`GlobalExceptionHandler` catches `NoSuchElementException` and `IllegalArgumentException` — both generic Java stdlib types. This creates two problems:

1. **Implicit coupling:** domain services must throw specific stdlib types to produce the correct HTTP status, an undeclared dependency between the domain and HTTP adapter layers.
2. **False-positive risk:** Spring internally throws `IllegalArgumentException` in several situations; catching it globally can turn framework errors into misleading 400 responses.

## Solution Overview

Introduce a small base exception hierarchy in `common/exception/` and concrete per-context exceptions in each bounded context's `domain/exception/` package. `GlobalExceptionHandler` catches only the three base types.

## Exception Hierarchy

### Base classes — `com.foodstock.common.exception`

| Class | Extends | HTTP status |
|---|---|---|
| `ResourceNotFoundException` (open) | `RuntimeException` | 404 Not Found |
| `InvalidOperationException` (open) | `RuntimeException` | 400 Bad Request |
| `UnauthorizedException` (open) | `RuntimeException` | 401 Unauthorized |

These are pure Kotlin classes with no Spring or framework imports.

### Per-context concrete exceptions

**`household.domain.exception`**

| Class | Extends | Thrown when |
|---|---|---|
| `HouseNotFoundException(houseId)` | `ResourceNotFoundException` | `houseRepository.findById` returns null |
| `UnauthorizedMemberOperationException(message)` | `InvalidOperationException` | non-owner attempts to invite a member |
| `AlreadyMemberException(message)` | `InvalidOperationException` | user is already a member of the house |

**`inventory.domain.exception`**

| Class | Extends | Thrown when |
|---|---|---|
| `ItemNotFoundException(itemId)` | `ResourceNotFoundException` | `inventoryRepository.findById` returns null |

**`auth.domain.exception`**

| Class | Extends | Thrown when |
|---|---|---|
| `EmailAlreadyInUseException(email)` | `InvalidOperationException` | duplicate email on register |
| `InvalidCredentialsException` | `UnauthorizedException` | wrong email or password on login |

## Updated GlobalExceptionHandler

Replace both stdlib handlers with:

```kotlin
@ExceptionHandler(ResourceNotFoundException::class)
fun handleNotFound(ex: ResourceNotFoundException) =
    ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

@ExceptionHandler(InvalidOperationException::class)
fun handleBadRequest(ex: InvalidOperationException) =
    ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

@ExceptionHandler(UnauthorizedException::class)
fun handleUnauthorized(ex: UnauthorizedException) =
    ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to ex.message))
```

## Domain Service Changes

| Service | Old throw | New throw |
|---|---|---|
| `HouseService.inviteMember` | `NoSuchElementException` | `HouseNotFoundException` |
| `HouseService.inviteMember` | `IllegalArgumentException` (owner check) | `UnauthorizedMemberOperationException` |
| `HouseService.inviteMember` | `IllegalArgumentException` (already member) | `AlreadyMemberException` |
| `InventoryService.updateQuantity` | `NoSuchElementException` | `ItemNotFoundException` |
| `AuthService.register` | `IllegalArgumentException` | `EmailAlreadyInUseException` |
| `AuthService.login` | `IllegalArgumentException` | `InvalidCredentialsException` |

## HTTP Status Changes

| Scenario | Before | After |
|---|---|---|
| Invalid credentials on login | 400 | **401** (more semantically correct) |
| All other mappings | unchanged | unchanged |

## Test Changes

Any existing test that asserts `assertThrows<NoSuchElementException>` or `assertThrows<IllegalArgumentException>` must be updated to the corresponding concrete exception type.

## Architecture Notes

- Base classes in `common/exception/` are pure Kotlin (no framework imports) — safe for domain layer consumption.
- Per-context exceptions live in `domain/exception/`, keeping them within their bounded context.
- `GlobalExceptionHandler` (adapter layer) imports only the three base types from `common/exception/`, never the concrete per-context types directly.
- No changes to domain models, ports, or JPA entities.
