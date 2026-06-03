# Household Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the household domain — add `InviteMemberUseCase`, refactor `HouseService` to follow the auth pattern (plain Kotlin class, no Spring annotations, wired via `@Configuration`), add the `HouseMember` JPA adapter, and expose an invite endpoint in the controller.

**Architecture:** Hexagonal — the domain service (`HouseService`) is a plain Kotlin class implementing use case interfaces. All Spring annotations live only in adapters (`@Repository`, `@Component`) and in the config class (`@Configuration` + `@Bean`), mirroring `AuthService`/`AuthConfig` exactly. The `createHouse` operation auto-creates an OWNER `HouseMember` record.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, Spring Data JPA, PostgreSQL (Flyway V1 schema already has `house_members` table), JUnit 5, Mockito-Kotlin 5.2.1

---

## File Map

**New files:**
- `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/InviteMemberUseCase.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/domain/port/out/HouseMemberRepository.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaEntity.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaRepository.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt`
- `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`

**Modified files:**
- `apps/api/build.gradle.kts` — add `mockito-kotlin` test dependency
- `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt` — remove `@Service`, add `HouseMemberRepository` dependency, implement `InviteMemberUseCase`, auto-create OWNER member in `createHouse`
- `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt` — add `POST /api/v1/houses/{houseId}/members` invite endpoint

---

## Task 1: Add test dependency (mockito-kotlin)

**Files:**
- Modify: `apps/api/build.gradle.kts`

- [ ] **Step 1: Add mockito-kotlin to test dependencies**

In `build.gradle.kts`, add inside the `dependencies { }` block, after the existing test lines:

```kotlin
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
```

The full test dependencies block should look like:
```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("org.springframework.security:spring-security-test")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
```

- [ ] **Step 2: Verify Gradle resolves the dependency**

Run from `apps/api/`:
```
./gradlew dependencies --configuration testRuntimeClasspath | grep mockito-kotlin
```
Expected output contains: `org.mockito.kotlin:mockito-kotlin:5.2.1`

- [ ] **Step 3: Commit**

```bash
git add apps/api/build.gradle.kts
git commit -m "chore(api/household): add mockito-kotlin for unit tests"
```

---

## Task 2: Add `InviteMemberUseCase` port and `HouseMemberRepository` port

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/InviteMemberUseCase.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/port/out/HouseMemberRepository.kt`

- [ ] **Step 1: Create `InviteMemberUseCase.kt`**

```kotlin
package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

data class InviteMemberCommand(
    val houseId: UUID,
    val invitedUserId: UUID,
    val invitedByUserId: UUID
)

interface InviteMemberUseCase {
    fun inviteMember(command: InviteMemberCommand): HouseMember
}
```

- [ ] **Step 2: Create `HouseMemberRepository.kt`**

```kotlin
package com.foodstock.household.domain.port.out

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

interface HouseMemberRepository {
    fun save(member: HouseMember): HouseMember
    fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember?
    fun findAllByHouseId(houseId: UUID): List<HouseMember>
}
```

- [ ] **Step 3: Verify compilation**

```
./gradlew compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/port/in/InviteMemberUseCase.kt
git add apps/api/src/main/kotlin/com/foodstock/household/domain/port/out/HouseMemberRepository.kt
git commit -m "feat(api/household): add InviteMemberUseCase and HouseMemberRepository ports"
```

---

## Task 3: Write failing unit tests for `HouseService`

**Files:**
- Create: `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`

> These tests will fail to compile until Task 4 because `HouseService` does not yet accept `HouseMemberRepository` or implement `InviteMemberUseCase`.

- [ ] **Step 1: Create the test directory structure**

```bash
mkdir -p apps/api/src/test/kotlin/com/foodstock/household/domain/service
```

- [ ] **Step 2: Create `HouseServiceTest.kt`**

```kotlin
package com.foodstock.household.domain.service

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class HouseServiceTest {

    private val houseRepository: HouseRepository = mock()
    private val houseMemberRepository: HouseMemberRepository = mock()
    private val service = HouseService(houseRepository, houseMemberRepository)

    @Test
    fun `createHouse saves house and creates OWNER member`() {
        val ownerId = UUID.randomUUID()
        val command = CreateHouseCommand(name = "Casa do Gustavo", ownerId = ownerId)
        whenever(houseRepository.save(any())).thenAnswer { it.arguments[0] as House }
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.createHouse(command)

        assertEquals("Casa do Gustavo", result.name)
        assertEquals(ownerId, result.ownerId)

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(ownerId, captor.firstValue.userId)
        assertEquals(MemberRole.OWNER, captor.firstValue.role)
        assertEquals(MemberStatus.ACTIVE, captor.firstValue.status)
    }

    @Test
    fun `inviteMember saves PENDING MEMBER when caller is owner`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, invitedUserId)).thenReturn(null)
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.inviteMember(
            InviteMemberCommand(houseId = houseId, invitedUserId = invitedUserId, invitedByUserId = ownerId)
        )

        assertEquals(houseId, result.houseId)
        assertEquals(invitedUserId, result.userId)
        assertEquals(MemberRole.MEMBER, result.role)
        assertEquals(MemberStatus.PENDING, result.status)
    }

    @Test
    fun `inviteMember throws NoSuchElementException when house not found`() {
        val command = InviteMemberCommand(
            houseId = UUID.randomUUID(),
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID()
        )
        whenever(houseRepository.findById(command.houseId)).thenReturn(null)

        assertThrows<NoSuchElementException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws IllegalArgumentException when caller is not the owner`() {
        val houseId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)

        val command = InviteMemberCommand(
            houseId = houseId,
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID() // different from ownerId
        )

        assertThrows<IllegalArgumentException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws IllegalArgumentException when user is already a member`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        val existingMember = HouseMember(
            id = UUID.randomUUID(), houseId = houseId, userId = invitedUserId,
            role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = LocalDateTime.now()
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, invitedUserId)).thenReturn(existingMember)

        val command = InviteMemberCommand(
            houseId = houseId, invitedUserId = invitedUserId, invitedByUserId = ownerId
        )

        assertThrows<IllegalArgumentException> { service.inviteMember(command) }
    }
}
```

- [ ] **Step 3: Run tests and confirm RED (compile error expected)**

```
./gradlew test --tests "com.foodstock.household.domain.service.HouseServiceTest"
```
Expected: compilation error — `HouseService` constructor does not accept `HouseMemberRepository` and does not implement `InviteMemberUseCase`.

- [ ] **Step 4: Commit the failing test**

```bash
git add apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt
git commit -m "test(api/household): add failing unit tests for HouseService"
```

---

## Task 4: Refactor `HouseService` — remove `@Service`, implement both use cases

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`

- [ ] **Step 1: Replace the full contents of `HouseService.kt`**

```kotlin
package com.foodstock.household.domain.service

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import java.time.LocalDateTime
import java.util.UUID

class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository
) : CreateHouseUseCase, InviteMemberUseCase {

    override fun createHouse(command: CreateHouseCommand): House {
        val now = LocalDateTime.now()
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
            ?: throw NoSuchElementException("House not found: ${command.houseId}")
        if (house.ownerId != command.invitedByUserId) {
            throw IllegalArgumentException("Only the house owner can invite members")
        }
        if (houseMemberRepository.findByHouseIdAndUserId(command.houseId, command.invitedUserId) != null) {
            throw IllegalArgumentException("User is already a member of this house")
        }
        return houseMemberRepository.save(
            HouseMember(
                id = UUID.randomUUID(),
                houseId = command.houseId,
                userId = command.invitedUserId,
                role = MemberRole.MEMBER,
                status = MemberStatus.PENDING,
                createdAt = LocalDateTime.now()
            )
        )
    }
}
```

- [ ] **Step 2: Run tests and confirm GREEN**

```
./gradlew test --tests "com.foodstock.household.domain.service.HouseServiceTest"
```
Expected: `5 tests completed, 0 failed — BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt
git commit -m "feat(api/household): refactor HouseService — remove @Service, implement InviteMemberUseCase"
```

---

## Task 5: Add `HouseMemberJpaEntity` and `HouseMemberJpaRepository`

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaEntity.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaRepository.kt`

The `house_members` table already exists in the Flyway V1 migration. No new migration needed.

- [ ] **Step 1: Create `HouseMemberJpaEntity.kt`**

```kotlin
package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "house_members")
class HouseMemberJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "house_id", nullable = false)
    val houseId: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole = MemberRole.MEMBER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MemberStatus = MemberStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): HouseMember = HouseMember(
        id = id, houseId = houseId, userId = userId,
        role = role, status = status, createdAt = createdAt
    )

    companion object {
        fun fromDomain(member: HouseMember) = HouseMemberJpaEntity(
            id = member.id, houseId = member.houseId, userId = member.userId,
            role = member.role, status = member.status, createdAt = member.createdAt
        )
    }
}
```

- [ ] **Step 2: Create `HouseMemberJpaRepository.kt`**

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

    override fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember? =
        delegate.findByHouseIdAndUserId(houseId, userId)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<HouseMember> =
        delegate.findAllByHouseId(houseId).map { it.toDomain() }
}
```

- [ ] **Step 3: Verify compilation**

```
./gradlew compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaEntity.kt
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/out/HouseMemberJpaRepository.kt
git commit -m "feat(api/household): add HouseMemberJpaEntity and HouseMemberJpaRepository"
```

---

## Task 6: Add `HouseholdConfig` to wire `HouseService` as a Spring bean

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt`

- [ ] **Step 1: Create `HouseholdConfig.kt`**

```kotlin
package com.foodstock.household.config

import com.foodstock.household.adapter.out.HouseJpaRepository
import com.foodstock.household.adapter.out.HouseMemberJpaRepository
import com.foodstock.household.domain.service.HouseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HouseholdConfig(
    private val houseJpaRepository: HouseJpaRepository,
    private val houseMemberJpaRepository: HouseMemberJpaRepository
) {
    @Bean
    fun houseService(): HouseService = HouseService(
        houseRepository = houseJpaRepository,
        houseMemberRepository = houseMemberJpaRepository
    )
}
```

- [ ] **Step 2: Verify compilation**

```
./gradlew compileKotlin
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/config/HouseholdConfig.kt
git commit -m "feat(api/household): add HouseholdConfig to wire HouseService bean"
```

---

## Task 7: Update `HouseController` — add invite member endpoint

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`

- [ ] **Step 1: Replace the full contents of `HouseController.kt`**

```kotlin
package com.foodstock.household.adapter.`in`

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateHouseRequest(val name: String)
data class InviteMemberRequest(val userId: UUID)

@RestController
@RequestMapping("/api/v1/houses")
class HouseController(
    private val createHouseUseCase: CreateHouseUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        // TODO: replace with @AuthenticationPrincipal once JWT filter is wired
        @RequestHeader("X-User-Id") ownerId: UUID
    ): House {
        return createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = ownerId)
        )
    }

    @PostMapping("/{houseId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteMember(
        @PathVariable houseId: UUID,
        @RequestBody request: InviteMemberRequest,
        // TODO: replace with @AuthenticationPrincipal once JWT filter is wired
        @RequestHeader("X-User-Id") invitedByUserId: UUID
    ): HouseMember {
        return inviteMemberUseCase.inviteMember(
            InviteMemberCommand(
                houseId = houseId,
                invitedUserId = request.userId,
                invitedByUserId = invitedByUserId
            )
        )
    }
}
```

- [ ] **Step 2: Verify full test suite still passes**

```
./gradlew test
```
Expected: all tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt
git commit -m "feat(api/household): add invite member endpoint POST /api/v1/houses/{houseId}/members"
```

---

## Self-Review Checklist

### Spec coverage
- [x] `House` domain model — already exists, unchanged
- [x] `HouseMember` domain model — already exists, unchanged
- [x] `CreateHouseUseCase` — exists, now implemented by refactored `HouseService`
- [x] `InviteMemberUseCase` — new port + implemented in `HouseService`
- [x] `HouseService` has no Spring annotations — plain class wired via `HouseholdConfig`
- [x] `HouseMember` JPA adapter — `HouseMemberJpaEntity` + `HouseMemberJpaRepository`
- [x] Controller exposes both use cases

### Placeholder scan
- No TBDs, no "similar to Task N", all code blocks are complete.

### Type consistency
- `InviteMemberCommand` defined in Task 2, used in Task 3 tests, Task 4 service, Task 7 controller — consistent.
- `HouseMemberRepository` defined in Task 2, mocked in Task 3, implemented in Task 5, injected in Task 6 — consistent.
- `MemberRole.OWNER/MEMBER`, `MemberStatus.ACTIVE/PENDING` — from existing `HouseMember.kt`, used consistently throughout.
