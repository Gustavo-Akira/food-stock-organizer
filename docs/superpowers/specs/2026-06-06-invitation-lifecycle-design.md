# Invitation Lifecycle Design

**Date:** 2026-06-06
**Scope:** `household` bounded context — accept, reject, and revoke house invitations

## Context

The invite flow already exists: `POST /api/v1/houses/{houseId}/members` creates a `HouseMember` with `status = PENDING`. The `MemberStatus` enum already declares `PENDING`, `ACTIVE`, and `REJECTED`, but no endpoint exists to transition out of `PENDING`. This spec adds that lifecycle.

## Requirements

- The invited user can **accept** or **reject** their own invitation.
- The house owner can **revoke** a pending invitation.
- No other actor may act on an invitation.
- Acting on an already-resolved invitation (status ≠ PENDING) is an error.

## API Contract

```
PATCH /api/v1/houses/{houseId}/members/{memberId}
Header: X-User-Id: <UUID>
Body:   { "action": "ACCEPT" | "REJECT" | "REVOKE" }

200 OK → HouseMemberResponse { id, houseId, userId, role, status }
```

Status transitions:
- `ACCEPT` → `ACTIVE`
- `REJECT` → `REJECTED`
- `REVOKE` → `REJECTED`

## Domain Layer

### New use case port — `RespondToInvitationUseCase`

File: `household/domain/port/in/RespondToInvitationUseCase.kt`

```kotlin
data class RespondToInvitationCommand(
    val houseId: UUID,
    val memberId: UUID,
    val respondingUserId: UUID,
    val action: InvitationAction
)

enum class InvitationAction { ACCEPT, REJECT, REVOKE }

interface RespondToInvitationUseCase {
    fun respondToInvitation(command: RespondToInvitationCommand): HouseMember
}
```

### `HouseService` — new method logic

1. Load house → `HouseNotFoundException` if absent
2. Load member by `memberId` → `InvitationNotFoundException` if absent
3. Guard `member.status == PENDING` → `InvitationAlreadyResolvedException` if not
4. Authorization:
   - `ACCEPT` / `REJECT`: `respondingUserId` must equal `member.userId`
   - `REVOKE`: `respondingUserId` must equal `house.ownerId`
   - Otherwise → `UnauthorizedMemberOperationException`
5. Save member with updated status and return it

### New domain exceptions

| Class | Extends | HTTP |
|---|---|---|
| `InvitationNotFoundException` | `ResourceNotFoundException` | 404 |
| `InvitationAlreadyResolvedException` | `InvalidOperationException` | 400 |

### `HouseMemberRepository` — new method

```kotlin
fun findById(memberId: UUID): HouseMember?
```

## Adapter Layer

### Request DTO

File: `household/adapter/in/dto/RespondToInvitationRequest.kt`

```kotlin
data class RespondToInvitationRequest(
    @field:NotNull val action: InvitationAction
)
```

### `HouseController` — new handler

```kotlin
@PatchMapping("/{houseId}/members/{memberId}")
fun respondToInvitation(
    @PathVariable houseId: UUID,
    @PathVariable memberId: UUID,
    @RequestBody request: RespondToInvitationRequest,
    @RequestHeader("X-User-Id") respondingUserId: UUID
): HouseMemberResponse
```

Returns existing `HouseMemberResponse` DTO (no new response type needed).

### `HouseholdConfig`

Expose a third `@Bean` of type `RespondToInvitationUseCase` backed by the same `HouseService` instance.

### `HouseMemberJpaRepository`

Add `findById` delegation (Spring Data `Optional` → nullable domain model).

## Testing

### `HouseServiceTest` additions

- `ACCEPT` transitions `PENDING → ACTIVE` when called by invited user
- `REJECT` transitions `PENDING → REJECTED` when called by invited user
- `REVOKE` transitions `PENDING → REJECTED` when called by house owner
- Throws `InvitationNotFoundException` when member not found
- Throws `InvitationAlreadyResolvedException` when status is not `PENDING`
- Throws `UnauthorizedMemberOperationException` when invited user tries `REVOKE`
- Throws `UnauthorizedMemberOperationException` when owner tries `ACCEPT` or `REJECT`

### `HouseControllerTest` additions

- `ACCEPT` returns 200 with updated `HouseMemberResponse`
- Missing action body returns 400
- Missing `X-User-Id` header returns 400

## Schema

No migration needed. `house_members.status` already stores `PENDING` / `ACTIVE` / `REJECTED` as strings and the column exists in `V1__create_initial_schema.sql`.
