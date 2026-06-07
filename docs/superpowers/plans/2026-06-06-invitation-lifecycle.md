# Invitation Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `PATCH /api/v1/houses/{houseId}/members/{memberId}` endpoint that allows the invited user to accept/reject their invitation and the house owner to revoke it.

**Architecture:** Single `RespondToInvitationUseCase` added to `HouseService`, following the existing hexagonal pattern — port interface in `domain/port/in`, implementation in `HouseService`, wired via `HouseholdConfig`. The `house_members.status` column already supports `PENDING`/`ACTIVE`/`REJECTED`; no migration needed.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, Spring MVC, JPA/Hibernate, JUnit 5, Mockito-Kotlin, MockMvc

---

## File Map

| Action | File |
|---|---|
| Create | `household/domain/exception/InvitationNotFoundException.kt` |
| Create | `household/domain/exception/InvitationAlreadyResolvedException.kt` |
| Create | `household/domain/port/in/RespondToInvitationUseCase.kt` |
| Create | `household/adapter/in/dto/RespondToInvitationRequest.kt` |
| Modify | `household/domain/port/out/HouseMemberRepository.kt` |
| Modify | `household/adapter/out/HouseMemberJpaRepository.kt` |
| Modify | `household/domain/service/HouseService.kt` |
| Modify | `household/adapter/in/HouseController.kt` |
| Modify | `household/domain/service/HouseServiceTest.kt` |
| Modify | `household/adapter/in/HouseControllerTest.kt` |

All paths are relative to `apps/api/src/main/kotlin/com/foodstock/` (or `src/test/kotlin/...` for tests).

---

## Task 1: Domain exceptions

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/InvitationNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/InvitationAlreadyResolvedException.kt`

- [ ] **Step 1: Create `InvitationNotFoundException`**

```kotlin
package com.foodstock.household.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class InvitationNotFoundException(memberId: UUID) : ResourceNotFoundException("Invitation not found: $memberId")
```

- [ ] **Step 2: Create `InvitationAlreadyResolvedException`**

```kotlin
package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class InvitationAlreadyResolvedException(message: String) : InvalidOperationException(message)
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/exception/InvitationNotFoundException.kt
git add apps/api/src/main/kotlin/com/foodstock/household/domain/exception/InvitationAlreadyResolvedException.kt
git commit -m "feat(household): add InvitationNotFoundException and InvitationAlreadyResolvedException"
```

---

## Task 2: RespondToInvitationUseCase port

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/RespondToInvitationUseCase.kt`

- [ ] **Step 1: Create the use case port**

```kotlin
package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

enum class InvitationAction { ACCEPT, REJECT, REVOKE }

data class RespondToInvitationCommand(
    val houseId: UUID,
    val memberId: UUID,
    val respondingUserId: UUID,
    val action: InvitationAction
)

interface RespondToInvitationUseCase {
    fun respondToInvitation(command: RespondToInvitationCommand): HouseMember
}
```

- [ ] **Step 2: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/RespondToInvitationUseCase.kt
git commit -m "feat(household): add RespondToInvitationUseCase port"
```

---

## Task 3: HouseMemberRepository findById

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/out/HouseMemberRepository.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaRepository.kt`

- [ ] **Step 1: Add `findById` to the port interface**

Replace the full file content:

```kotlin
package com.foodstock.household.domain.port.out

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

interface HouseMemberRepository {
    fun save(member: HouseMember): HouseMember
    fun findById(memberId: UUID): HouseMember?
    fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember?
    fun findAllByHouseId(houseId: UUID): List<HouseMember>
}
```

- [ ] **Step 2: Implement `findById` in the JPA adapter**

`HouseMemberJpaRepositoryDelegate` already inherits `findById(UUID): Optional<HouseMemberJpaEntity>` from `JpaRepository`. Add only the override in `HouseMemberJpaRepository`:

Replace the full file content:

```kotlin
package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.port.out.HouseMemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface HouseMemberJpaRepositoryDelegate : JpaRepository<HouseMemberJpaEntity, UUID> {
    fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMemberJpaEntity?
    fun findAllByHouseId(houseId: UUID): List<HouseMemberJpaEntity>
}

@Repository
class HouseMemberJpaRepository(
    private val delegate: HouseMemberJpaRepositoryDelegate
) : HouseMemberRepository {

    override fun save(member: HouseMember): HouseMember =
        delegate.save(HouseMemberJpaEntity.fromDomain(member)).toDomain()

    override fun findById(memberId: UUID): HouseMember? =
        delegate.findById(memberId).orElse(null)?.toDomain()

    override fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember? =
        delegate.findByHouseIdAndUserId(houseId, userId)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<HouseMember> =
        delegate.findAllByHouseId(houseId).map { it.toDomain() }
}
```

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/port/out/HouseMemberRepository.kt
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaRepository.kt
git commit -m "feat(household): add findById to HouseMemberRepository"
```

---

## Task 4: HouseService — TDD respondToInvitation

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`

- [ ] **Step 1: Write failing tests — add to `HouseServiceTest`**

Add these imports at the top of the existing import block:

```kotlin
import com.foodstock.household.domain.exception.InvitationAlreadyResolvedException
import com.foodstock.household.domain.exception.InvitationNotFoundException
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
```

Add these test methods to the `HouseServiceTest` class body:

```kotlin
@Test
fun `respondToInvitation ACCEPT transitions PENDING to ACTIVE when called by invited user`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
    whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

    val result = service.respondToInvitation(
        RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.ACCEPT)
    )

    val captor = argumentCaptor<HouseMember>()
    verify(houseMemberRepository).save(captor.capture())
    assertEquals(MemberStatus.ACTIVE, captor.firstValue.status)
    assertEquals(MemberStatus.ACTIVE, result.status)
}

@Test
fun `respondToInvitation REJECT transitions PENDING to REJECTED when called by invited user`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
    whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

    val result = service.respondToInvitation(
        RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.REJECT)
    )

    val captor = argumentCaptor<HouseMember>()
    verify(houseMemberRepository).save(captor.capture())
    assertEquals(MemberStatus.REJECTED, captor.firstValue.status)
    assertEquals(MemberStatus.REJECTED, result.status)
}

@Test
fun `respondToInvitation REVOKE transitions PENDING to REJECTED when called by house owner`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
    whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

    val result = service.respondToInvitation(
        RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = ownerId, action = InvitationAction.REVOKE)
    )

    val captor = argumentCaptor<HouseMember>()
    verify(houseMemberRepository).save(captor.capture())
    assertEquals(MemberStatus.REJECTED, captor.firstValue.status)
    assertEquals(MemberStatus.REJECTED, result.status)
}

@Test
fun `respondToInvitation throws HouseNotFoundException when house not found`() {
    whenever(houseRepository.findById(any())).thenReturn(null)

    assertThrows<HouseNotFoundException> {
        service.respondToInvitation(
            RespondToInvitationCommand(houseId = UUID.randomUUID(), memberId = UUID.randomUUID(), respondingUserId = UUID.randomUUID(), action = InvitationAction.ACCEPT)
        )
    }
}

@Test
fun `respondToInvitation throws InvitationNotFoundException when member not found`() {
    val houseId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = expectedNow, updatedAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(any())).thenReturn(null)

    assertThrows<InvitationNotFoundException> {
        service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = UUID.randomUUID(), respondingUserId = UUID.randomUUID(), action = InvitationAction.ACCEPT)
        )
    }
}

@Test
fun `respondToInvitation throws InvitationAlreadyResolvedException when status is ACTIVE`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val activeMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(activeMember)

    assertThrows<InvitationAlreadyResolvedException> {
        service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.ACCEPT)
        )
    }
}

@Test
fun `respondToInvitation throws UnauthorizedMemberOperationException when invited user tries REVOKE`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)

    assertThrows<UnauthorizedMemberOperationException> {
        service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.REVOKE)
        )
    }
}

@Test
fun `respondToInvitation throws UnauthorizedMemberOperationException when owner tries ACCEPT`() {
    val ownerId = UUID.randomUUID()
    val invitedUserId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val expectedNow = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
    val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)

    assertThrows<UnauthorizedMemberOperationException> {
        service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = ownerId, action = InvitationAction.ACCEPT)
        )
    }
}
```

- [ ] **Step 2: Run tests — verify they fail to compile**

```
cd apps/api
./gradlew test --tests "*.HouseServiceTest" 2>&1 | head -30
```

Expected: compilation error — `respondToInvitation` is unresolved and `findById` is unresolved on the mock.

- [ ] **Step 3: Implement `respondToInvitation` in `HouseService`**

Replace the full file:

```kotlin
package com.foodstock.household.domain.service

import com.foodstock.household.domain.exception.AlreadyMemberException
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.exception.InvitationAlreadyResolvedException
import com.foodstock.household.domain.exception.InvitationNotFoundException
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase, RespondToInvitationUseCase {

    override fun createHouse(command: CreateHouseCommand): House {
        val now = LocalDateTime.now(clock)
        val house = House(
            id = UUID.randomUUID(),
            name = command.name,
            ownerId = command.ownerId,
            createdAt = now,
            updatedAt = now
        )
        val savedHouse = houseRepository.save(house)
        houseMemberRepository.save(
            HouseMember(
                id = UUID.randomUUID(),
                houseId = savedHouse.id,
                userId = command.ownerId,
                role = MemberRole.OWNER,
                status = MemberStatus.ACTIVE,
                createdAt = now
            )
        )
        return savedHouse
    }

    override fun inviteMember(command: InviteMemberCommand): HouseMember {
        val house = houseRepository.findById(command.houseId)
            ?: throw HouseNotFoundException(command.houseId)
        if (house.ownerId != command.invitedByUserId) {
            throw UnauthorizedMemberOperationException("Only the house owner can invite members")
        }
        if (houseMemberRepository.findByHouseIdAndUserId(command.houseId, command.invitedUserId) != null) {
            throw AlreadyMemberException("User is already a member of this house")
        }
        val now = LocalDateTime.now(clock)
        return houseMemberRepository.save(
            HouseMember(
                id = UUID.randomUUID(),
                houseId = command.houseId,
                userId = command.invitedUserId,
                role = MemberRole.MEMBER,
                status = MemberStatus.PENDING,
                createdAt = now
            )
        )
    }

    override fun respondToInvitation(command: RespondToInvitationCommand): HouseMember {
        val house = houseRepository.findById(command.houseId)
            ?: throw HouseNotFoundException(command.houseId)
        val member = houseMemberRepository.findById(command.memberId)
            ?: throw InvitationNotFoundException(command.memberId)
        if (member.status != MemberStatus.PENDING) {
            throw InvitationAlreadyResolvedException("Invitation is already ${member.status}")
        }
        when (command.action) {
            InvitationAction.ACCEPT, InvitationAction.REJECT -> {
                if (command.respondingUserId != member.userId) {
                    throw UnauthorizedMemberOperationException("Only the invited user can accept or reject an invitation")
                }
            }
            InvitationAction.REVOKE -> {
                if (command.respondingUserId != house.ownerId) {
                    throw UnauthorizedMemberOperationException("Only the house owner can revoke an invitation")
                }
            }
        }
        val newStatus = when (command.action) {
            InvitationAction.ACCEPT -> MemberStatus.ACTIVE
            InvitationAction.REJECT, InvitationAction.REVOKE -> MemberStatus.REJECTED
        }
        return houseMemberRepository.save(member.copy(status = newStatus))
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```
cd apps/api
./gradlew test --tests "*.HouseServiceTest"
```

Expected: BUILD SUCCESSFUL, all tests GREEN.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt
git add apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt
git commit -m "feat(household): implement RespondToInvitationUseCase in HouseService"
```

---

## Task 5: HouseController — TDD PATCH endpoint

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/RespondToInvitationRequest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`

- [ ] **Step 1: Create the request DTO**

```kotlin
package com.foodstock.household.adapter.`in`.dto

import com.foodstock.household.domain.port.`in`.InvitationAction

data class RespondToInvitationRequest(val action: InvitationAction)
```

- [ ] **Step 2: Write failing controller tests — add to `HouseControllerTest`**

Add these imports to the existing import block in `HouseControllerTest`:

```kotlin
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.springframework.test.web.servlet.patch
```

Add this `@MockBean` field inside the `HouseControllerTest` class:

```kotlin
@MockBean
private lateinit var respondToInvitationUseCase: RespondToInvitationUseCase
```

Add these test methods to the `HouseControllerTest` class body:

```kotlin
@Test
fun `respondToInvitation returns 200 with updated member on ACCEPT`() {
    val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    val now = LocalDateTime.parse("2026-06-06T12:00:00")
    whenever(respondToInvitationUseCase.respondToInvitation(any())).thenReturn(
        HouseMember(id = memberId, houseId = houseId, userId = userId, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE, createdAt = now)
    )

    mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
        contentType = MediaType.APPLICATION_JSON
        header("X-User-Id", userId.toString())
        content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
    }
        .andExpect {
            status { isOk() }
            jsonPath("$.id") { value(memberId.toString()) }
            jsonPath("$.houseId") { value(houseId.toString()) }
            jsonPath("$.userId") { value(userId.toString()) }
            jsonPath("$.status") { value("ACTIVE") }
        }
}

@Test
fun `respondToInvitation returns 400 when action body is missing`() {
    val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")

    mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
        contentType = MediaType.APPLICATION_JSON
        header("X-User-Id", userId.toString())
        content = "{}"
    }
        .andExpect {
            status { isBadRequest() }
        }
}

@Test
fun `respondToInvitation returns 400 when X-User-Id header is missing`() {
    val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
    }
        .andExpect {
            status { isBadRequest() }
        }
}
```

- [ ] **Step 3: Run tests — verify they fail**

```
cd apps/api
./gradlew test --tests "*.HouseControllerTest"
```

Expected: compilation error — `respondToInvitation` endpoint not found / `RespondToInvitationUseCase` not wired in controller.

- [ ] **Step 4: Update `HouseController` with the new endpoint**

Replace the full file:

```kotlin
package com.foodstock.household.adapter.`in`

import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class HouseResponse(val id: UUID, val name: String, val ownerId: UUID)
data class HouseMemberResponse(
    val id: UUID,
    val houseId: UUID,
    val userId: UUID,
    val role: MemberRole,
    val status: MemberStatus
)

@RestController
@RequestMapping("/api/v1/houses")
class HouseController(
    private val createHouseUseCase: CreateHouseUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val respondToInvitationUseCase: RespondToInvitationUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        @RequestHeader("X-User-Id") ownerId: UUID
    ): HouseResponse {
        val house = createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = ownerId)
        )
        return HouseResponse(id = house.id, name = house.name, ownerId = house.ownerId)
    }

    @PostMapping("/{houseId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteMember(
        @PathVariable houseId: UUID,
        @RequestBody request: InviteMemberRequest,
        @RequestHeader("X-User-Id") invitedByUserId: UUID
    ): HouseMemberResponse {
        val member = inviteMemberUseCase.inviteMember(
            InviteMemberCommand(
                houseId = houseId,
                invitedUserId = request.userId,
                invitedByUserId = invitedByUserId
            )
        )
        return HouseMemberResponse(
            id = member.id,
            houseId = member.houseId,
            userId = member.userId,
            role = member.role,
            status = member.status
        )
    }

    @PatchMapping("/{houseId}/members/{memberId}")
    fun respondToInvitation(
        @PathVariable houseId: UUID,
        @PathVariable memberId: UUID,
        @RequestBody request: RespondToInvitationRequest,
        @RequestHeader("X-User-Id") respondingUserId: UUID
    ): HouseMemberResponse {
        val member = respondToInvitationUseCase.respondToInvitation(
            RespondToInvitationCommand(
                houseId = houseId,
                memberId = memberId,
                respondingUserId = respondingUserId,
                action = request.action
            )
        )
        return HouseMemberResponse(
            id = member.id,
            houseId = member.houseId,
            userId = member.userId,
            role = member.role,
            status = member.status
        )
    }
}
```

- [ ] **Step 5: Run tests — verify they pass**

```
cd apps/api
./gradlew test --tests "*.HouseControllerTest"
```

Expected: BUILD SUCCESSFUL, all tests GREEN.

- [ ] **Step 6: Run full check — verify coverage gate passes**

```
cd apps/api
./gradlew check
```

Expected: BUILD SUCCESSFUL — all tests pass and JaCoCo coverage gate met.

- [ ] **Step 7: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/RespondToInvitationRequest.kt
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt
git add apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt
git commit -m "feat(household): add PATCH /houses/{houseId}/members/{memberId} invitation response endpoint"
```
