# DTO Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract inbound request and response DTOs from REST controllers into bounded-context `adapter/in/dto` packages while preserving controller behavior and adding MVC slice coverage.

**Architecture:** DTOs move one class per file under each context's inbound adapter layer, and controllers import those DTOs instead of declaring them inline. The inventory domain-to-HTTP mapper becomes a top-level extension beside `InventoryItemResponse` because it must be visible from `InventoryController` across the `dto` package boundary.

**Tech Stack:** Kotlin 1.9.24, Spring Boot 3.3.0, Spring MVC, Jakarta Validation, JUnit 5, MockMvc, Mockito Kotlin, JaCoCo.

---

## Implementation Notes

- The design spec says to use `@MockitoBean`; this project currently uses Spring Boot `3.3.0`, where `@MockBean` is the available Spring Boot test annotation. Use `@MockBean` unless the project is intentionally upgraded to a Spring Boot version that provides `@MockitoBean`.
- Add `@ResponseStatus(HttpStatus.CREATED)` to `AuthController.register` so `POST /api/auth/register` matches the spec's `201` expectation.
- Keep `HouseController` and `ShoppingListController` returning domain models. The spec marks that leakage as out of scope.
- Keep all new imports in `adapter/in` or `adapter/in/dto`; no domain package should import DTOs or Spring MVC types.

## File Structure

- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/RegisterRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/UserResponse.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginResponse.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/AuthControllerTest.kt`

- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/CreateHouseRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/InviteMemberRequest.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`

- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/AddItemRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/UpdateQuantityRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/InventoryItemResponse.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`

- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/GenerateShoppingListRequest.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`

---

### Task 1: Auth DTO Extraction and Controller Test

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/RegisterRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/UserResponse.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginResponse.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/AuthControllerTest.kt`

- [ ] **Step 1: Write the failing auth controller test**

Create `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/AuthControllerTest.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.`in`.LoginResult
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var registerUseCase: RegisterUseCase

    @MockBean
    private lateinit var loginUseCase: LoginUseCase

    @Test
    fun `register returns created user`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        whenever(registerUseCase.register(any(), any(), any())).thenReturn(
            User(id = userId, name = "Ana", email = "ana@example.com", passwordHash = "hash")
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("name" to "Ana", "email" to "ana@example.com", "password" to "secret")
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(userId.toString()) }
                jsonPath("$.name") { value("Ana") }
                jsonPath("$.email") { value("ana@example.com") }
            }
    }

    @Test
    fun `login returns token and user`() {
        val userId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(loginUseCase.login(any(), any())).thenReturn(
            LoginResult(
                token = "jwt-token",
                user = User(id = userId, name = "Ana", email = "ana@example.com", passwordHash = "hash")
            )
        )

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "ana@example.com", "password" to "secret")
            )
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.token") { value("jwt-token") }
                jsonPath("$.user.id") { value(userId.toString()) }
                jsonPath("$.user.email") { value("ana@example.com") }
            }
    }

    @Test
    fun `register rejects invalid body`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = "{"
        }
            .andExpect {
                status { isBadRequest() }
            }
    }
}
```

- [ ] **Step 2: Run the auth test to verify it fails**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.auth.adapter.in.AuthControllerTest
```

Expected: FAIL because `AuthControllerTest.kt` imports DTO/controller behavior that has not been extracted yet and `register` still returns HTTP 200.

- [ ] **Step 3: Create auth DTO files**

Create `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/RegisterRequest.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`.dto

data class RegisterRequest(val name: String, val email: String, val password: String)
```

Create `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginRequest.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`.dto

data class LoginRequest(val email: String, val password: String)
```

Create `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/UserResponse.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`.dto

data class UserResponse(val id: String, val name: String, val email: String)
```

Create `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto/LoginResponse.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`.dto

data class LoginResponse(val token: String, val user: UserResponse)
```

- [ ] **Step 4: Update `AuthController` to import DTOs and return 201 for register**

Replace the DTO declarations and imports in `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthController.kt` with:

```kotlin
package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.adapter.`in`.dto.LoginRequest
import com.foodstock.auth.adapter.`in`.dto.LoginResponse
import com.foodstock.auth.adapter.`in`.dto.RegisterRequest
import com.foodstock.auth.adapter.`in`.dto.UserResponse
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): UserResponse {
        val user = registerUseCase.register(request.name, request.email, request.password)
        return UserResponse(user.id.toString(), user.name, user.email)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = loginUseCase.login(request.email, request.password)
        return ResponseEntity.ok(
            LoginResponse(
                token = result.token,
                user = UserResponse(result.user.id.toString(), result.user.name, result.user.email)
            )
        )
    }
}
```

- [ ] **Step 5: Run the auth test to verify it passes**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.auth.adapter.in.AuthControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit auth extraction**

Run:

```powershell
git add apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthController.kt apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/dto apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/AuthControllerTest.kt
git commit -m "refactor(auth): extract controller dtos"
```

### Task 2: Household DTO Extraction and Controller Test

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/CreateHouseRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/InviteMemberRequest.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`

- [ ] **Step 1: Write the failing household controller test**

Create `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`:

```kotlin
package com.foodstock.household.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(HouseController::class)
@AutoConfigureMockMvc(addFilters = false)
class HouseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var createHouseUseCase: CreateHouseUseCase

    @MockBean
    private lateinit var inviteMemberUseCase: InviteMemberUseCase

    @Test
    fun `createHouse returns created house`() {
        val houseId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val ownerId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val now = LocalDateTime.parse("2026-06-05T12:00:00")
        whenever(createHouseUseCase.createHouse(any())).thenReturn(
            House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = now, updatedAt = now)
        )

        mockMvc.post("/api/v1/houses") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", ownerId.toString())
            content = objectMapper.writeValueAsString(mapOf("name" to "Casa"))
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(houseId.toString()) }
                jsonPath("$.name") { value("Casa") }
                jsonPath("$.ownerId") { value(ownerId.toString()) }
            }
    }

    @Test
    fun `inviteMember returns created member`() {
        val memberId = UUID.fromString("55555555-5555-5555-5555-555555555555")
        val houseId = UUID.fromString("66666666-6666-6666-6666-666666666666")
        val invitedUserId = UUID.fromString("77777777-7777-7777-7777-777777777777")
        val invitedByUserId = UUID.fromString("88888888-8888-8888-8888-888888888888")
        whenever(inviteMemberUseCase.inviteMember(any())).thenReturn(
            HouseMember(
                id = memberId,
                houseId = houseId,
                userId = invitedUserId,
                role = MemberRole.MEMBER,
                status = MemberStatus.PENDING,
                createdAt = LocalDateTime.parse("2026-06-05T12:00:00")
            )
        )

        mockMvc.post("/api/v1/houses/$houseId/members") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", invitedByUserId.toString())
            content = objectMapper.writeValueAsString(mapOf("userId" to invitedUserId))
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(memberId.toString()) }
                jsonPath("$.houseId") { value(houseId.toString()) }
                jsonPath("$.userId") { value(invitedUserId.toString()) }
                jsonPath("$.status") { value("PENDING") }
            }
    }

    @Test
    fun `createHouse rejects missing user header`() {
        mockMvc.post("/api/v1/houses") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("name" to "Casa"))
        }
            .andExpect {
                status { isBadRequest() }
            }
    }
}
```

- [ ] **Step 2: Run the household test to verify it fails**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.household.adapter.in.HouseControllerTest
```

Expected: FAIL until DTO imports are moved to `household.adapter.in.dto`.

- [ ] **Step 3: Create household DTO files**

Create `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/CreateHouseRequest.kt`:

```kotlin
package com.foodstock.household.adapter.`in`.dto

data class CreateHouseRequest(val name: String)
```

Create `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto/InviteMemberRequest.kt`:

```kotlin
package com.foodstock.household.adapter.`in`.dto

import java.util.UUID

data class InviteMemberRequest(val userId: UUID)
```

- [ ] **Step 4: Update `HouseController` imports and remove inline DTOs**

At the top of `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`, add:

```kotlin
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
```

Delete these two top-level declarations from the controller:

```kotlin
data class CreateHouseRequest(val name: String)
data class InviteMemberRequest(val userId: UUID)
```

- [ ] **Step 5: Run the household test to verify it passes**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.household.adapter.in.HouseControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit household extraction**

Run:

```powershell
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt apps/api/src/main/kotlin/com/foodstock/household/adapter/in/dto apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt
git commit -m "refactor(household): extract controller dtos"
```

### Task 3: Inventory DTO Extraction, Mapper Move, and Controller Test

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/AddItemRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/UpdateQuantityRequest.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/InventoryItemResponse.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`

- [ ] **Step 1: Write the failing inventory controller test**

Create `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(InventoryController::class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var addItemUseCase: AddItemUseCase

    @MockBean
    private lateinit var updateItemQuantityUseCase: UpdateItemQuantityUseCase

    @Test
    fun `addItem returns created inventory item response`() {
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(addItemUseCase.addItem(any())).thenReturn(item)

        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", item.houseId.toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Arroz",
                    "category" to "FOOD",
                    "quantityLevel" to "PLENTY",
                    "expiryDate" to "2026-12-31",
                    "notes" to "Pacote fechado"
                )
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(item.id.toString()) }
                jsonPath("$.houseId") { value(item.houseId.toString()) }
                jsonPath("$.name") { value("Arroz") }
                jsonPath("$.category") { value("FOOD") }
                jsonPath("$.quantityLevel") { value("PLENTY") }
                jsonPath("$.expiryDate") { value("2026-12-31") }
                jsonPath("$.notes") { value("Pacote fechado") }
            }
    }

    @Test
    fun `updateQuantity returns updated inventory item response`() {
        val itemId = UUID.fromString("99999999-9999-9999-9999-999999999999")
        val item = inventoryItem(id = itemId, quantityLevel = QuantityLevel.RUNNING_OUT)
        whenever(updateItemQuantityUseCase.updateQuantity(any())).thenReturn(item)

        mockMvc.patch("/api/v1/inventory/$itemId/quantity") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("quantityLevel" to "RUNNING_OUT"))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(itemId.toString()) }
                jsonPath("$.quantityLevel") { value("RUNNING_OUT") }
            }
    }

    @Test
    fun `addItem rejects blank name`() {
        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "",
                    "category" to "FOOD",
                    "quantityLevel" to "PLENTY",
                    "expiryDate" to null,
                    "notes" to null
                )
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `addItem rejects null quantity level`() {
        mockMvc.post("/api/v1/inventory") {
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", UUID.randomUUID().toString())
            content = objectMapper.writeValueAsString(
                mapOf(
                    "name" to "Arroz",
                    "category" to "FOOD",
                    "quantityLevel" to null,
                    "expiryDate" to null,
                    "notes" to null
                )
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    private fun inventoryItem(
        id: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        houseId: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        quantityLevel: QuantityLevel
    ) = InventoryItem(
        id = id,
        houseId = houseId,
        name = "Arroz",
        category = Category.FOOD,
        quantityLevel = quantityLevel,
        expiryDate = LocalDate.parse("2026-12-31"),
        notes = "Pacote fechado",
        createdAt = LocalDateTime.parse("2026-06-05T12:00:00"),
        updatedAt = LocalDateTime.parse("2026-06-05T12:30:00")
    )
}
```

- [ ] **Step 2: Run the inventory test to verify it fails**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.inventory.adapter.in.InventoryControllerTest
```

Expected: FAIL until DTOs and `toResponse()` are moved into the `dto` package.

- [ ] **Step 3: Create inventory request DTO files**

Create `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/AddItemRequest.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.QuantityLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AddItemRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,
    @field:NotNull
    val category: Category,
    @field:NotNull
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    @field:Size(max = 1000)
    val notes: String?
)
```

Create `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/UpdateQuantityRequest.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.QuantityLevel
import jakarta.validation.constraints.NotNull

data class UpdateQuantityRequest(
    @field:NotNull
    val quantityLevel: QuantityLevel
)
```

- [ ] **Step 4: Create inventory response DTO and mapper**

Create `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto/InventoryItemResponse.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`.dto

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class InventoryItemResponse(
    val id: UUID,
    val houseId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

fun InventoryItem.toResponse() = InventoryItemResponse(
    id = id,
    houseId = houseId,
    name = name,
    category = category,
    quantityLevel = quantityLevel,
    expiryDate = expiryDate,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
```

- [ ] **Step 5: Update `InventoryController` imports and remove inline DTOs**

At the top of `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`, keep only these imports needed by the controller:

```kotlin
import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.InventoryItemResponse
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.adapter.`in`.dto.toResponse
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
```

Delete the inline `AddItemRequest`, `UpdateQuantityRequest`, `InventoryItemResponse`, and private `InventoryItem.toResponse()` declarations from the controller file.

- [ ] **Step 6: Run the inventory test to verify it passes**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.inventory.adapter.in.InventoryControllerTest
```

Expected: PASS.

- [ ] **Step 7: Commit inventory extraction**

Run:

```powershell
git add apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/dto apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt
git commit -m "refactor(inventory): extract controller dtos"
```

### Task 4: Shopping DTO Extraction and Controller Test

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt`
- Create: `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/GenerateShoppingListRequest.kt`
- Test: `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`

- [ ] **Step 1: Write the failing shopping controller test**

Create `apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt`:

```kotlin
package com.foodstock.shopping.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.`in`.GenerateShoppingListUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(ShoppingListController::class)
@AutoConfigureMockMvc(addFilters = false)
class ShoppingListControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var generateShoppingListUseCase: GenerateShoppingListUseCase

    @Test
    fun `generateList returns created shopping list`() {
        val listId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val houseId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        val now = LocalDateTime.parse("2026-06-05T12:00:00")
        whenever(generateShoppingListUseCase.generateFromRunningOutItems(any())).thenReturn(
            ShoppingList(
                id = listId,
                houseId = houseId,
                name = "Lista semanal",
                status = ShoppingListStatus.OPEN,
                createdBy = userId,
                createdAt = now,
                updatedAt = now
            )
        )

        mockMvc.post("/api/v1/shopping-lists/generate") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            content = objectMapper.writeValueAsString(
                mapOf("houseId" to houseId, "listName" to "Lista semanal")
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(listId.toString()) }
                jsonPath("$.houseId") { value(houseId.toString()) }
                jsonPath("$.name") { value("Lista semanal") }
                jsonPath("$.status") { value("OPEN") }
            }
    }

    @Test
    fun `generateList rejects missing user header`() {
        mockMvc.post("/api/v1/shopping-lists/generate") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("houseId" to UUID.randomUUID(), "listName" to "Lista semanal")
            )
        }
            .andExpect {
                status { isBadRequest() }
            }
    }
}
```

- [ ] **Step 2: Run the shopping test to verify it fails**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.shopping.adapter.in.ShoppingListControllerTest
```

Expected: FAIL until `GenerateShoppingListRequest` is imported from `shopping.adapter.in.dto`.

- [ ] **Step 3: Create shopping DTO file**

Create `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto/GenerateShoppingListRequest.kt`:

```kotlin
package com.foodstock.shopping.adapter.`in`.dto

import java.util.UUID

data class GenerateShoppingListRequest(
    val houseId: UUID,
    val listName: String = "Lista de Compras"
)
```

- [ ] **Step 4: Update `ShoppingListController` imports and remove inline DTO**

At the top of `apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt`, add:

```kotlin
import com.foodstock.shopping.adapter.`in`.dto.GenerateShoppingListRequest
```

Delete this top-level declaration from the controller:

```kotlin
data class GenerateShoppingListRequest(
    val houseId: UUID,
    val listName: String = "Lista de Compras"
)
```

- [ ] **Step 5: Run the shopping test to verify it passes**

Run:

```powershell
cd apps/api
.\gradlew.bat test --tests com.foodstock.shopping.adapter.in.ShoppingListControllerTest
```

Expected: PASS.

- [ ] **Step 6: Commit shopping extraction**

Run:

```powershell
git add apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/ShoppingListController.kt apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in/dto apps/api/src/test/kotlin/com/foodstock/shopping/adapter/in/ShoppingListControllerTest.kt
git commit -m "refactor(shopping): extract controller dtos"
```

### Task 5: Full Backend Verification and Coverage Check

**Files:**
- Verify: `apps/api/build/reports/jacoco/test/html/index.html`
- Verify: `apps/api/build/reports/jacoco/test/jacocoTestReport.xml`

- [ ] **Step 1: Run all backend tests and JaCoCo**

Run:

```powershell
cd apps/api
.\gradlew.bat test
```

Expected: BUILD SUCCESSFUL, and JaCoCo XML/HTML reports generated.

- [ ] **Step 2: Run Gradle check**

Run:

```powershell
cd apps/api
.\gradlew.bat check
```

Expected: BUILD SUCCESSFUL with the configured 80% aggregate JaCoCo rule passing.

- [ ] **Step 3: Inspect package boundaries**

Run:

```powershell
rg "adapter\\.`in`\\.dto|adapter\\.in\\.dto" apps/api/src/main/kotlin/com/foodstock/*/domain apps/api/src/main/kotlin/com/foodstock/*/domain/*
```

Expected: no matches, confirming domain code does not import inbound DTOs.

- [ ] **Step 4: Confirm controllers contain no DTO declarations**

Run:

```powershell
rg "^data class .*Request|^data class .*Response|private fun InventoryItem\\.toResponse" apps/api/src/main/kotlin/com/foodstock/*/adapter/in/*Controller.kt
```

Expected: no matches.

- [ ] **Step 5: Confirm DTO files exist in the expected packages**

Run:

```powershell
rg --files apps/api/src/main/kotlin/com/foodstock | rg "adapter\\\\in\\\\dto\\\\.*\\.kt$"
```

Expected output includes:

```text
apps\api\src\main\kotlin\com\foodstock\auth\adapter\in\dto\RegisterRequest.kt
apps\api\src\main\kotlin\com\foodstock\auth\adapter\in\dto\LoginRequest.kt
apps\api\src\main\kotlin\com\foodstock\auth\adapter\in\dto\UserResponse.kt
apps\api\src\main\kotlin\com\foodstock\auth\adapter\in\dto\LoginResponse.kt
apps\api\src\main\kotlin\com\foodstock\household\adapter\in\dto\CreateHouseRequest.kt
apps\api\src\main\kotlin\com\foodstock\household\adapter\in\dto\InviteMemberRequest.kt
apps\api\src\main\kotlin\com\foodstock\inventory\adapter\in\dto\AddItemRequest.kt
apps\api\src\main\kotlin\com\foodstock\inventory\adapter\in\dto\UpdateQuantityRequest.kt
apps\api\src\main\kotlin\com\foodstock\inventory\adapter\in\dto\InventoryItemResponse.kt
apps\api\src\main\kotlin\com\foodstock\shopping\adapter\in\dto\GenerateShoppingListRequest.kt
```

- [ ] **Step 6: Commit verification adjustments if any were needed**

Run this only if verification required a code or test correction:

```powershell
git add apps/api/src/main/kotlin apps/api/src/test/kotlin
git commit -m "test(api): cover extracted controller dtos"
```

Expected: either a commit is created for real corrections, or no command is run because Tasks 1-4 already contain all implementation and tests.

### Task 6: Final Review

**Files:**
- Review: all modified files from `git diff --stat main...HEAD`

- [ ] **Step 1: Review changed files**

Run:

```powershell
git diff --stat main...HEAD
```

Expected: changes are limited to controller files, new `adapter/in/dto` files, and new controller tests.

- [ ] **Step 2: Review DTO extraction diff**

Run:

```powershell
git diff main...HEAD -- apps/api/src/main/kotlin/com/foodstock/auth/adapter/in apps/api/src/main/kotlin/com/foodstock/household/adapter/in apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in apps/api/src/main/kotlin/com/foodstock/shopping/adapter/in
```

Expected: DTO declarations are removed from controllers and recreated unchanged in `dto` files, except `AuthController.register` now returns HTTP 201 and inventory `toResponse()` is top-level in `InventoryItemResponse.kt`.

- [ ] **Step 3: Prepare PR summary**

Use this PR summary:

```markdown
## Summary
- Extracted REST request/response DTOs from controller files into per-context `adapter/in/dto` packages.
- Moved inventory `InventoryItem.toResponse()` into the inventory DTO package as a top-level extension.
- Added MVC slice coverage for auth, household, inventory, and shopping controllers.

## Verification
- `cd apps/api && .\gradlew.bat test`
- `cd apps/api && .\gradlew.bat check`
```

---

## Self-Review

- Spec coverage: The plan extracts all DTOs named in the spec, moves inventory `toResponse()` out of the controller, keeps domain leakage in household/shopping out of scope, and adds one controller test class per bounded context with the requested status scenarios.
- Placeholder scan: The plan contains no deferred implementation markers or unspecified edge-case instructions.
- Type consistency: DTO names, package names, use-case method names, domain model properties, enum values, and endpoint paths match the current source files inspected before writing this plan.
