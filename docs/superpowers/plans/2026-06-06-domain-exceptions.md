# Domain Exceptions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace generic Java stdlib exceptions with custom domain exception types so `GlobalExceptionHandler` decouples from stdlib and domain services have explicit, type-safe contracts.

**Architecture:** Three open base classes live in `common/exception/` (pure Kotlin, no Spring). Per-context concrete exceptions live in each bounded context's `domain/exception/` and extend these bases. `GlobalExceptionHandler` catches only the three base types — it never imports per-context exceptions directly.

**Tech Stack:** Kotlin 1.9, Spring Boot 3.3, JUnit 5, Mockito-Kotlin, MockMvc (`@WebMvcTest`)

---

## File Map

**Create (source):**
- `apps/api/src/main/kotlin/com/foodstock/common/exception/ResourceNotFoundException.kt`
- `apps/api/src/main/kotlin/com/foodstock/common/exception/InvalidOperationException.kt`
- `apps/api/src/main/kotlin/com/foodstock/common/exception/UnauthorizedException.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/HouseNotFoundException.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/UnauthorizedMemberOperationException.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/AlreadyMemberException.kt`
- `apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/ItemNotFoundException.kt`
- `apps/api/src/main/kotlin/com/foodstock/auth/domain/exception/EmailAlreadyInUseException.kt`
- `apps/api/src/main/kotlin/com/foodstock/auth/domain/exception/InvalidCredentialsException.kt`

**Modify (source):**
- `apps/api/src/main/kotlin/com/foodstock/common/GlobalExceptionHandler.kt`
- `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`
- `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`
- `apps/api/src/main/kotlin/com/foodstock/auth/domain/service/AuthService.kt`

**Modify (tests):**
- `apps/api/src/test/kotlin/com/foodstock/common/GlobalExceptionHandlerTest.kt`
- `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`
- `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`
- `apps/api/src/test/kotlin/com/foodstock/auth/domain/service/AuthServiceTest.kt`

---

### Task 1: Create base exception classes

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/common/exception/ResourceNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/common/exception/InvalidOperationException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/common/exception/UnauthorizedException.kt`

- [ ] **Step 1: Create feature branch**

```bash
git checkout -b fix/replace-stdlib-exceptions-with-domain-exceptions
```

- [ ] **Step 2: Create `ResourceNotFoundException.kt`**

```kotlin
package com.foodstock.common.exception

open class ResourceNotFoundException(message: String) : RuntimeException(message)
```

- [ ] **Step 3: Create `InvalidOperationException.kt`**

```kotlin
package com.foodstock.common.exception

open class InvalidOperationException(message: String) : RuntimeException(message)
```

- [ ] **Step 3: Create `UnauthorizedException.kt`**

```kotlin
package com.foodstock.common.exception

open class UnauthorizedException(message: String) : RuntimeException(message)
```

- [ ] **Step 4: Compile to confirm no errors**

```bash
cd apps/api && ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/common/exception/
git commit -m "feat(api): add base domain exception hierarchy"
```

---

### Task 2: Create per-context domain exceptions

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/HouseNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/UnauthorizedMemberOperationException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/domain/exception/AlreadyMemberException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/ItemNotFoundException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/domain/exception/EmailAlreadyInUseException.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/domain/exception/InvalidCredentialsException.kt`

- [ ] **Step 1: Create `HouseNotFoundException.kt`**

```kotlin
package com.foodstock.household.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class HouseNotFoundException(houseId: UUID) : ResourceNotFoundException("House not found: $houseId")
```

- [ ] **Step 2: Create `UnauthorizedMemberOperationException.kt`**

```kotlin
package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class UnauthorizedMemberOperationException(message: String) : InvalidOperationException(message)
```

- [ ] **Step 3: Create `AlreadyMemberException.kt`**

```kotlin
package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class AlreadyMemberException(message: String) : InvalidOperationException(message)
```

- [ ] **Step 4: Create `ItemNotFoundException.kt`**

```kotlin
package com.foodstock.inventory.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ItemNotFoundException(itemId: UUID) : ResourceNotFoundException("Item not found: $itemId")
```

- [ ] **Step 5: Create `EmailAlreadyInUseException.kt`**

```kotlin
package com.foodstock.auth.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class EmailAlreadyInUseException(email: String) : InvalidOperationException("Email already in use: $email")
```

- [ ] **Step 6: Create `InvalidCredentialsException.kt`**

```kotlin
package com.foodstock.auth.domain.exception

import com.foodstock.common.exception.UnauthorizedException

class InvalidCredentialsException : UnauthorizedException("Invalid credentials")
```

- [ ] **Step 7: Compile to confirm no errors**

```bash
cd apps/api && ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/exception/
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/
git add apps/api/src/main/kotlin/com/foodstock/auth/domain/exception/
git commit -m "feat(api): add per-context domain exceptions for household, inventory, and auth"
```

---

### Task 3: Update GlobalExceptionHandler (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/common/GlobalExceptionHandlerTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/common/GlobalExceptionHandler.kt`

- [ ] **Step 1: Update the test (failing step)**

Replace the full contents of `GlobalExceptionHandlerTest.kt`:

```kotlin
package com.foodstock.common

import com.foodstock.common.exception.InvalidOperationException
import com.foodstock.common.exception.ResourceNotFoundException
import com.foodstock.common.exception.UnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ExceptionStubController {
    @GetMapping("/test/not-found")
    fun throwNotFound(): String = throw ResourceNotFoundException("item not found")

    @GetMapping("/test/bad-request")
    fun throwBadRequest(): String = throw InvalidOperationException("invalid input")

    @GetMapping("/test/unauthorized")
    fun throwUnauthorized(): String = throw UnauthorizedException("unauthorized")
}

@WebMvcTest(ExceptionStubController::class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ResourceNotFoundException is mapped to 404 with error body`() {
        mockMvc.get("/test/not-found")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("item not found") }
            }
    }

    @Test
    fun `InvalidOperationException is mapped to 400 with error body`() {
        mockMvc.get("/test/bad-request")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("invalid input") }
            }
    }

    @Test
    fun `UnauthorizedException is mapped to 401 with error body`() {
        mockMvc.get("/test/unauthorized")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("unauthorized") }
            }
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.GlobalExceptionHandlerTest"
```

Expected: FAIL — 3 tests fail with HTTP 500 (handler still catches stdlib types, not domain types)

- [ ] **Step 3: Update `GlobalExceptionHandler.kt`**

Replace the full contents:

```kotlin
package com.foodstock.common

import com.foodstock.common.exception.InvalidOperationException
import com.foodstock.common.exception.ResourceNotFoundException
import com.foodstock.common.exception.UnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidOperationException::class)
    fun handleBadRequest(ex: InvalidOperationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to ex.message))
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.GlobalExceptionHandlerTest"
```

Expected: 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/common/GlobalExceptionHandler.kt
git add apps/api/src/test/kotlin/com/foodstock/common/GlobalExceptionHandlerTest.kt
git commit -m "refactor(api): replace stdlib exception handlers with domain exception types in GlobalExceptionHandler"
```

---

### Task 4: Update HouseService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt`

- [ ] **Step 1: Update the three exception tests in `HouseServiceTest.kt`**

Replace only the three failing-exception test methods (lines 97–149). The rest of the file stays identical:

```kotlin
    @Test
    fun `inviteMember throws HouseNotFoundException when house not found`() {
        val command = InviteMemberCommand(
            houseId = UUID.randomUUID(),
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID()
        )
        whenever(houseRepository.findById(command.houseId)).thenReturn(null)

        assertThrows<HouseNotFoundException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws UnauthorizedMemberOperationException when caller is not the owner`() {
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
            invitedByUserId = UUID.randomUUID()
        )

        assertThrows<UnauthorizedMemberOperationException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws AlreadyMemberException when user is already a member`() {
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

        assertThrows<AlreadyMemberException> { service.inviteMember(command) }
    }
```

Also add imports at the top of the test file:

```kotlin
import com.foodstock.household.domain.exception.AlreadyMemberException
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest"
```

Expected: 3 exception tests FAIL (service still throws `NoSuchElementException` / `IllegalArgumentException`)

- [ ] **Step 3: Update `HouseService.kt`**

Replace the `inviteMember` method body and add imports:

```kotlin
package com.foodstock.household.domain.service

import com.foodstock.household.domain.exception.AlreadyMemberException
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
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
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase {

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
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.HouseServiceTest"
```

Expected: all 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/household/domain/service/HouseService.kt
git add apps/api/src/test/kotlin/com/foodstock/household/domain/service/HouseServiceTest.kt
git commit -m "refactor(household): replace stdlib exceptions with domain exceptions in HouseService"
```

---

### Task 5: Update InventoryService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`

- [ ] **Step 1: Update the exception test in `InventoryServiceTest.kt`**

Replace only the `updateQuantity throws` test method (lines 104–112):

```kotlin
    @Test
    fun `updateQuantity throws ItemNotFoundException when item not found`() {
        val itemId = UUID.randomUUID()
        whenever(inventoryRepository.findById(itemId)).thenReturn(null)

        assertThrows<ItemNotFoundException> {
            service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.ENOUGH))
        }
    }
```

Also add this import at the top of the test file:

```kotlin
import com.foodstock.inventory.domain.exception.ItemNotFoundException
```

- [ ] **Step 2: Run the tests to verify the updated test fails**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryServiceTest"
```

Expected: `updateQuantity throws ItemNotFoundException` FAILS (service still throws `NoSuchElementException`)

- [ ] **Step 3: Update `InventoryService.kt`**

Replace the `updateQuantity` method and add the import:

```kotlin
package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import com.foodstock.inventory.domain.port.out.InventoryRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val clock: Clock
) : AddItemUseCase, UpdateItemQuantityUseCase {

    override fun addItem(command: AddItemCommand): InventoryItem {
        val now = LocalDateTime.now(clock)
        val item = InventoryItem(
            id = UUID.randomUUID(),
            houseId = command.houseId,
            name = command.name,
            category = command.category,
            quantityLevel = command.quantityLevel,
            expiryDate = command.expiryDate,
            notes = command.notes,
            createdAt = now,
            updatedAt = now
        )
        return inventoryRepository.save(item)
    }

    override fun updateQuantity(command: UpdateItemQuantityCommand): InventoryItem {
        val item = inventoryRepository.findById(command.itemId)
            ?: throw ItemNotFoundException(command.itemId)
        val updated = item.copy(
            quantityLevel = command.quantityLevel,
            updatedAt = LocalDateTime.now(clock)
        )
        return inventoryRepository.save(updated)
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.InventoryServiceTest"
```

Expected: all 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt
git add apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt
git commit -m "refactor(inventory): replace stdlib exceptions with domain exceptions in InventoryService"
```

---

### Task 6: Update AuthService (TDD)

**Files:**
- Modify: `apps/api/src/test/kotlin/com/foodstock/auth/domain/service/AuthServiceTest.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/domain/service/AuthService.kt`

- [ ] **Step 1: Update the three exception tests in `AuthServiceTest.kt`**

Replace only these three test methods while keeping the rest unchanged:

```kotlin
    @Test
    fun `register throws EmailAlreadyInUseException when email already in use`() {
        whenever(userRepository.existsByEmail("alice@example.com")).thenReturn(true)

        assertThrows<EmailAlreadyInUseException> {
            service.register(name = "Alice", email = "alice@example.com", password = "secret")
        }
    }

    @Test
    fun `login throws InvalidCredentialsException when user not found`() {
        whenever(userRepository.findByEmail("unknown@example.com")).thenReturn(null)

        val ex = assertThrows<InvalidCredentialsException> {
            service.login(email = "unknown@example.com", password = "secret")
        }
        assertEquals("Invalid credentials", ex.message)
    }

    @Test
    fun `login throws InvalidCredentialsException when password does not match`() {
        val user = User(name = "Alice", email = "alice@example.com", passwordHash = "hashed")
        whenever(userRepository.findByEmail("alice@example.com")).thenReturn(user)
        whenever(passwordHashPort.matches("wrong", "hashed")).thenReturn(false)

        val ex = assertThrows<InvalidCredentialsException> {
            service.login(email = "alice@example.com", password = "wrong")
        }
        assertEquals("Invalid credentials", ex.message)
    }
```

Also add these imports at the top of the test file:

```kotlin
import com.foodstock.auth.domain.exception.EmailAlreadyInUseException
import com.foodstock.auth.domain.exception.InvalidCredentialsException
```

- [ ] **Step 2: Run the tests to verify they fail**

```bash
cd apps/api && ./gradlew test --tests "*.AuthServiceTest"
```

Expected: 3 exception tests FAIL (service still throws `IllegalArgumentException`)

- [ ] **Step 3: Update `AuthService.kt`**

Replace the full file contents:

```kotlin
package com.foodstock.auth.domain.service

import com.foodstock.auth.domain.exception.EmailAlreadyInUseException
import com.foodstock.auth.domain.exception.InvalidCredentialsException
import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.`in`.LoginResult
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import com.foodstock.auth.domain.port.out.JwtPort
import com.foodstock.auth.domain.port.out.PasswordHashPort
import com.foodstock.auth.domain.port.out.UserRepository

class AuthService(
    private val userRepository: UserRepository,
    private val passwordHashPort: PasswordHashPort,
    private val jwtPort: JwtPort
) : RegisterUseCase, LoginUseCase {

    override fun register(name: String, email: String, password: String): User {
        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyInUseException(email)
        }
        val user = User(
            name = name,
            email = email,
            passwordHash = passwordHashPort.hash(password)
        )
        return userRepository.save(user)
    }

    override fun login(email: String, password: String): LoginResult {
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException()
        if (!passwordHashPort.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        val token = jwtPort.generateToken(user)
        return LoginResult(token = token, user = user)
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
cd apps/api && ./gradlew test --tests "*.AuthServiceTest"
```

Expected: all 5 tests PASS

- [ ] **Step 5: Commit**

```bash
git add apps/api/src/main/kotlin/com/foodstock/auth/domain/service/AuthService.kt
git add apps/api/src/test/kotlin/com/foodstock/auth/domain/service/AuthServiceTest.kt
git commit -m "refactor(auth): replace stdlib exceptions with domain exceptions in AuthService"
```

---

### Task 7: Full check and branch setup

- [ ] **Step 1: Run the full check gate**

```bash
cd apps/api && ./gradlew check
```

Expected: `BUILD SUCCESSFUL` — all tests pass, JaCoCo coverage gate passes

- [ ] **Step 2: If any test fails, investigate the failure message before changing code**

Common issues:
- Compilation error in test: missing import — add the domain exception import to the test file
- Wrong exception type asserted: double-check the test uses the exact class name from `domain/exception/`
- JaCoCo gate fails: new exception classes need coverage — the service tests already exercise them via `assertThrows`, so coverage should be fine

- [ ] **Step 3: Create the branch (if not already on a feature branch)**

```bash
git checkout -b fix/replace-stdlib-exceptions-with-domain-exceptions
```

Note: if the commits above were made on `main`, rebase them onto this branch instead.

- [ ] **Step 4: Push and open PR**

```bash
git push -u origin fix/replace-stdlib-exceptions-with-domain-exceptions
gh pr create \
  --title "refactor(api): replace stdlib exceptions with custom domain exceptions" \
  --body "$(cat <<'EOF'
## Summary

- Introduces `ResourceNotFoundException`, `InvalidOperationException`, and `UnauthorizedException` base classes in `common/exception/`
- Adds per-context concrete exceptions in `household/domain/exception/`, `inventory/domain/exception/`, and `auth/domain/exception/`
- Updates `GlobalExceptionHandler` to catch domain base types instead of `NoSuchElementException` / `IllegalArgumentException`
- Updates all four domain services (`HouseService`, `InventoryService`, `AuthService`) to throw domain exceptions
- Changes login failure response from 400 → 401 (semantically correct for auth failures)

Closes #20

## Test plan

- [ ] `./gradlew check` passes locally
- [ ] `GlobalExceptionHandlerTest` verifies 404 / 400 / 401 mappings via `@WebMvcTest`
- [ ] `HouseServiceTest`, `InventoryServiceTest`, `AuthServiceTest` assert domain exception types
- [ ] No stdlib `NoSuchElementException` or `IllegalArgumentException` remain in domain services
EOF
)"
```
