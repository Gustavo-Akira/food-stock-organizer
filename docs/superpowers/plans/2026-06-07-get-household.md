# GET Household Endpoints Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `GET /api/v1/houses`, `GET /api/v1/houses/{houseId}`, and `GET /api/v1/houses/{houseId}/members` endpoints to the household bounded context.

**Architecture:** Three new use case interfaces in `domain/port/in`, implemented by `HouseService` (no Spring annotations), wired as bean aliases in `HouseholdConfig`, and exposed via new `@GetMapping` methods in `HouseController`. Authorization for single-house and members endpoints: caller must have an `ACTIVE` membership record for that house.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, JUnit 5, Mockito Kotlin, MockMvc (`@WebMvcTest`)

**Branch:** `feat/get-household` (create a git worktree before starting)

---

## File Map

| Action | Path |
|--------|------|
| CREATE | `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetMyHousesUseCase.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseUseCase.kt` |
| CREATE | `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseMembersUseCase.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt` |
| MODIFY | `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt` |
| MODIFY | `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt` |

---

## Task 1: Create use case interfaces

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetMyHousesUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseMembersUseCase.kt`

- [ ] **Step 1: Create GetMyHousesUseCase**

```kotlin
package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.House
import java.util.UUID

interface GetMyHousesUseCase {
    fun getMyHouses(userId: UUID): List<House>
}
```

- [ ] **Step 2: Create GetHouseUseCase**

```kotlin
package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.House
import java.util.UUID

interface GetHouseUseCase {
    fun getHouse(houseId: UUID, requestingUserId: UUID): House
}
```

- [ ] **Step 3: Create GetHouseMembersUseCase**

```kotlin
package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

interface GetHouseMembersUseCase {
    fun getHouseMembers(houseId: UUID, requestingUserId: UUID): List<HouseMember>
}
```

- [ ] **Step 4: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetMyHousesUseCase.kt \
        apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseUseCase.kt \
        apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/GetHouseMembersUseCase.kt
git commit -m "feat(household): add GetMyHouses, GetHouse, GetHouseMembers use case interfaces"
```

---

## Task 2: Implement getMyHouses in HouseService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`

- [ ] **Step 1: Add failing tests to HouseServiceTest**

Add these two test methods to the existing `HouseServiceTest` class:

```kotlin
@Test
fun `getMyHouses returns houses owned by user`() {
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = UUID.randomUUID(), name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
    whenever(houseRepository.findAllByOwnerId(userId)).thenReturn(listOf(house))

    val result = service.getMyHouses(userId)

    assertEquals(1, result.size)
    assertEquals(house.id, result[0].id)
    assertEquals(userId, result[0].ownerId)
}

@Test
fun `getMyHouses returns empty list when user owns no houses`() {
    val userId = UUID.randomUUID()
    whenever(houseRepository.findAllByOwnerId(userId)).thenReturn(emptyList())

    val result = service.getMyHouses(userId)

    assertEquals(0, result.size)
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `getMyHouses` not found on `HouseService`.

- [ ] **Step 3: Implement getMyHouses in HouseService**

Update the class declaration to add `GetMyHousesUseCase` and add the import and method:

```kotlin
// Add to imports:
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase

// Change class declaration:
class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase, RespondToInvitationUseCase, GetMyHousesUseCase {

// Add method (before the closing brace):
    override fun getMyHouses(userId: UUID): List<House> =
        houseRepository.findAllByOwnerId(userId)
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt \
        apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt
git commit -m "feat(household): implement getMyHouses in HouseService"
```

---

## Task 3: Implement getHouse and getHouseMembers in HouseService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`

- [ ] **Step 1: Add failing tests to HouseServiceTest**

Add these test methods:

```kotlin
@Test
fun `getHouse returns house for active member`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
    val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.OWNER, status = MemberStatus.ACTIVE, createdAt = now)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

    val result = service.getHouse(houseId, userId)

    assertEquals(houseId, result.id)
    assertEquals("Casa", result.name)
}

@Test
fun `getHouse throws HouseNotFoundException when house does not exist`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    whenever(houseRepository.findById(houseId)).thenReturn(null)

    assertThrows<HouseNotFoundException> { service.getHouse(houseId, userId) }
}

@Test
fun `getHouse throws UnauthorizedMemberOperationException when user has no membership`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

    assertThrows<UnauthorizedMemberOperationException> { service.getHouse(houseId, userId) }
}

@Test
fun `getHouse throws UnauthorizedMemberOperationException when member is not ACTIVE`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
    val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = now)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

    assertThrows<UnauthorizedMemberOperationException> { service.getHouse(houseId, userId) }
}

@Test
fun `getHouseMembers returns members for active member`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
    val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.OWNER, status = MemberStatus.ACTIVE, createdAt = now)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)
    whenever(houseMemberRepository.findAllByHouseId(houseId)).thenReturn(listOf(member))

    val result = service.getHouseMembers(houseId, userId)

    assertEquals(1, result.size)
    assertEquals(userId, result[0].userId)
    assertEquals(MemberStatus.ACTIVE, result[0].status)
}

@Test
fun `getHouseMembers throws HouseNotFoundException when house does not exist`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    whenever(houseRepository.findById(houseId)).thenReturn(null)

    assertThrows<HouseNotFoundException> { service.getHouseMembers(houseId, userId) }
}

@Test
fun `getHouseMembers throws UnauthorizedMemberOperationException when user is not active member`() {
    val houseId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val now = LocalDateTime.now(fixedClock)
    val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
    whenever(houseRepository.findById(houseId)).thenReturn(house)
    whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

    assertThrows<UnauthorizedMemberOperationException> { service.getHouseMembers(houseId, userId) }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest" 2>&1 | tail -20
```

Expected: compilation error — `getHouse` and `getHouseMembers` not found.

- [ ] **Step 3: Implement getHouse, getHouseMembers, and private helper in HouseService**

Add these imports to `HouseService.kt`:

```kotlin
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
```

Update the class declaration to add the new interfaces:

```kotlin
class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase, RespondToInvitationUseCase,
    GetMyHousesUseCase, GetHouseUseCase, GetHouseMembersUseCase {
```

Add these methods before the closing brace:

```kotlin
    override fun getHouse(houseId: UUID, requestingUserId: UUID): House {
        val house = houseRepository.findById(houseId) ?: throw HouseNotFoundException(houseId)
        checkActiveMember(houseId, requestingUserId)
        return house
    }

    override fun getHouseMembers(houseId: UUID, requestingUserId: UUID): List<HouseMember> {
        houseRepository.findById(houseId) ?: throw HouseNotFoundException(houseId)
        checkActiveMember(houseId, requestingUserId)
        return houseMemberRepository.findAllByHouseId(houseId)
    }

    private fun checkActiveMember(houseId: UUID, userId: UUID) {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
        if (member == null || member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedMemberOperationException("Only active house members can view this resource")
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt \
        apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt
git commit -m "feat(household): implement getHouse and getHouseMembers in HouseService"
```

---

## Task 4: Wire new use cases in HouseholdConfig

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt`

- [ ] **Step 1: Add imports and three new bean aliases to HouseholdConfig**

Add to the imports section:

```kotlin
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
```

Add three new `@Bean` methods inside the class (after the existing `respondToInvitationUseCase()` bean):

```kotlin
    @Bean
    fun getMyHousesUseCase(): GetMyHousesUseCase = houseService()

    @Bean
    fun getHouseUseCase(): GetHouseUseCase = houseService()

    @Bean
    fun getHouseMembersUseCase(): GetHouseMembersUseCase = houseService()
```

- [ ] **Step 2: Run full test suite to confirm nothing broke**

```bash
cd apps/api && ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt
git commit -m "feat(household): register GetMyHouses, GetHouse, GetHouseMembers beans in HouseholdConfig"
```

---

## Task 5: Add GET endpoints to HouseController (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`

- [ ] **Step 1: Add failing tests to HouseControllerTest**

Add the three new `@MockBean` fields after the existing ones:

```kotlin
    @MockBean
    private lateinit var getMyHousesUseCase: GetMyHousesUseCase

    @MockBean
    private lateinit var getHouseUseCase: GetHouseUseCase

    @MockBean
    private lateinit var getHouseMembersUseCase: GetHouseMembersUseCase
```

Add these imports to HouseControllerTest:

```kotlin
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import org.springframework.test.web.servlet.get
```

Add these test methods:

```kotlin
    @Test
    fun `getMyHouses returns list of houses`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        whenever(getMyHousesUseCase.getMyHouses(userId)).thenReturn(
            listOf(House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now))
        )

        mockMvc.get("/api/v1/houses") {
            header("X-User-Id", userId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(houseId.toString()) }
                jsonPath("$[0].name") { value("Casa") }
                jsonPath("$[0].ownerId") { value(userId.toString()) }
            }
    }

    @Test
    fun `getMyHouses returns 400 when X-User-Id header is missing`() {
        mockMvc.get("/api/v1/houses")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getHouse returns house for active member`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        whenever(getHouseUseCase.getHouse(houseId, userId)).thenReturn(
            House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
        )

        mockMvc.get("/api/v1/houses/$houseId") {
            header("X-User-Id", userId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(houseId.toString()) }
                jsonPath("$.name") { value("Casa") }
                jsonPath("$.ownerId") { value(userId.toString()) }
            }
    }

    @Test
    fun `getHouse returns 404 when house does not exist`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(getHouseUseCase.getHouse(houseId, userId)).thenThrow(HouseNotFoundException(houseId))

        mockMvc.get("/api/v1/houses/$houseId") {
            header("X-User-Id", userId.toString())
        }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `getHouse returns 403 when user is not active member`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(getHouseUseCase.getHouse(houseId, userId))
            .thenThrow(UnauthorizedMemberOperationException("Only active house members can view this resource"))

        mockMvc.get("/api/v1/houses/$houseId") {
            header("X-User-Id", userId.toString())
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Only active house members can view this resource") }
            }
    }

    @Test
    fun `getHouseMembers returns members list`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val memberId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        whenever(getHouseMembersUseCase.getHouseMembers(houseId, userId)).thenReturn(
            listOf(HouseMember(id = memberId, houseId = houseId, userId = userId, role = MemberRole.OWNER, status = MemberStatus.ACTIVE, createdAt = now))
        )

        mockMvc.get("/api/v1/houses/$houseId/members") {
            header("X-User-Id", userId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(memberId.toString()) }
                jsonPath("$[0].userId") { value(userId.toString()) }
                jsonPath("$[0].status") { value("ACTIVE") }
                jsonPath("$[0].role") { value("OWNER") }
            }
    }

    @Test
    fun `getHouseMembers returns 404 when house does not exist`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(getHouseMembersUseCase.getHouseMembers(houseId, userId)).thenThrow(HouseNotFoundException(houseId))

        mockMvc.get("/api/v1/houses/$houseId/members") {
            header("X-User-Id", userId.toString())
        }
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `getHouseMembers returns 403 when user is not active member`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(getHouseMembersUseCase.getHouseMembers(houseId, userId))
            .thenThrow(UnauthorizedMemberOperationException("Only active house members can view this resource"))

        mockMvc.get("/api/v1/houses/$houseId/members") {
            header("X-User-Id", userId.toString())
        }
            .andExpect { status { isForbidden() } }
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.HouseControllerTest" 2>&1 | tail -20
```

Expected: compilation error or bean not found — new use cases not in controller yet.

- [ ] **Step 3: Add GET endpoints and new use case dependencies to HouseController**

Add these imports to `HouseController.kt`:

```kotlin
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import org.springframework.web.bind.annotation.GetMapping
```

Update the constructor to add the three new use cases:

```kotlin
@RestController
@RequestMapping("/api/v1/houses")
class HouseController(
    private val createHouseUseCase: CreateHouseUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val respondToInvitationUseCase: RespondToInvitationUseCase,
    private val getMyHousesUseCase: GetMyHousesUseCase,
    private val getHouseUseCase: GetHouseUseCase,
    private val getHouseMembersUseCase: GetHouseMembersUseCase
) {
```

Add these three methods inside the class body (after the existing `respondToInvitation` method):

```kotlin
    @GetMapping
    fun getMyHouses(
        @RequestHeader("X-User-Id") userId: UUID
    ): List<HouseResponse> =
        getMyHousesUseCase.getMyHouses(userId).map { HouseResponse(it.id, it.name, it.ownerId) }

    @GetMapping("/{houseId}")
    fun getHouse(
        @PathVariable houseId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): HouseResponse {
        val house = getHouseUseCase.getHouse(houseId, userId)
        return HouseResponse(house.id, house.name, house.ownerId)
    }

    @GetMapping("/{houseId}/members")
    fun getHouseMembers(
        @PathVariable houseId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): List<HouseMemberResponse> =
        getHouseMembersUseCase.getHouseMembers(houseId, userId)
            .map { HouseMemberResponse(it.id, it.houseId, it.userId, it.role, it.status) }
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.HouseControllerTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Run full test suite**

```bash
cd apps/api && ./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt \
        apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt
git commit -m "feat(household): add GET /houses, /houses/{id}, /houses/{id}/members endpoints"
```
