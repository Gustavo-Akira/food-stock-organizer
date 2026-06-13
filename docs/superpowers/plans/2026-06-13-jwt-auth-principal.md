# JWT Authentication Principal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire a JWT authentication filter, replace raw `X-User-Id`/`X-House-Id` header stubs with `@AuthenticationPrincipal`, and verify house membership in the inventory domain before every operation.

**Architecture:** A `JwtAuthenticationFilter` (`OncePerRequestFilter`) reads the Bearer token, validates it, and sets a custom `AuthenticatedUser` principal in `SecurityContextHolder`. Controllers receive the principal via `@AuthenticationPrincipal`. The inventory context gains a `HouseMemberCheckPort` (same pattern as shopping's `MemberRolePort`) to guard all four inventory operations.

**Tech Stack:** Spring Security, jjwt 0.12.5, Kotlin, Spring MVC, Mockito Kotlin, spring-security-test

---

## File Map

| Action | Path |
|--------|------|
| Modify | `apps/api/src/main/kotlin/com/foodstock/auth/domain/port/out/AuthPorts.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/auth/adapter/out/JwtAdapter.kt` |
| Create | `apps/api/src/test/kotlin/com/foodstock/auth/adapter/out/JwtAdapterTest.kt` |
| Create | `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthenticatedUser.kt` |
| Create | `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilter.kt` |
| Create | `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilterTest.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/auth/config/AuthConfig.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/auth/config/SecurityConfig.kt` |
| Create | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/HouseAccessDeniedException.kt` |
| Create | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/out/HouseMemberCheckPort.kt` |
| Create | `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/out/HouseMemberCheckAdapter.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/AddItemUseCase.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/UpdateItemQuantityUseCase.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt` |
| Modify | `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt` |
| Modify | `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt` |
| Modify | `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt` |
| Modify | `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt` |

---

### Task 1: Add `extractUserId` to `JwtPort` and `JwtAdapter`

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/domain/port/out/AuthPorts.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/out/JwtAdapter.kt`
- Create: `apps/api/src/test/kotlin/com/foodstock/auth/adapter/out/JwtAdapterTest.kt`

- [ ] **Step 1: Write failing test for `extractUserId`**

Create `apps/api/src/test/kotlin/com/foodstock/auth/adapter/out/JwtAdapterTest.kt`:

```kotlin
package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtAdapterTest {

    private val secret = "test-secret-key-that-is-at-least-32-characters-long"
    private val adapter = JwtAdapter(secret, expirationMs = 3_600_000)
    private val user = User(
        id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        name = "Test User",
        email = "test@example.com",
        passwordHash = "hash"
    )

    @Test
    fun `extractUserId returns the userId embedded in the token`() {
        val token = adapter.generateToken(user)
        val result = adapter.extractUserId(token)
        assertEquals(user.id, result)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```
cd apps/api && ./gradlew test --tests "*.JwtAdapterTest"
```

Expected: compile error — `extractUserId` does not exist yet.

- [ ] **Step 3: Add `extractUserId` to `JwtPort` interface**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/auth/domain/port/out/AuthPorts.kt`:

```kotlin
package com.foodstock.auth.domain.port.out

import com.foodstock.auth.domain.model.User
import java.util.UUID

interface PasswordHashPort {
    fun hash(raw: String): String
    fun matches(raw: String, hashed: String): Boolean
}

interface JwtPort {
    fun generateToken(user: User): String
    fun validateToken(token: String): Boolean
    fun extractEmail(token: String): String
    fun extractUserId(token: String): UUID
}
```

- [ ] **Step 4: Implement `extractUserId` in `JwtAdapter`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/auth/adapter/out/JwtAdapter.kt`:

```kotlin
package com.foodstock.auth.adapter.out

import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.out.JwtPort
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtAdapter(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.expiration-ms:86400000}") private val expirationMs: Long
) : JwtPort {

    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    override fun generateToken(user: User): String = Jwts.builder()
        .subject(user.email)
        .claim("userId", user.id.toString())
        .claim("name", user.name)
        .issuedAt(Date())
        .expiration(Date(System.currentTimeMillis() + expirationMs))
        .signWith(key)
        .compact()

    override fun validateToken(token: String): Boolean = runCatching {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    }.getOrDefault(false)

    override fun extractEmail(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject

    override fun extractUserId(token: String): UUID =
        UUID.fromString(
            Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).payload["userId"] as String
        )
}
```

- [ ] **Step 5: Run test to confirm it passes**

```
cd apps/api && ./gradlew test --tests "*.JwtAdapterTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/auth/domain/port/out/AuthPorts.kt \
        apps/api/src/main/kotlin/com/foodstock/auth/adapter/out/JwtAdapter.kt \
        apps/api/src/test/kotlin/com/foodstock/auth/adapter/out/JwtAdapterTest.kt
git commit -m "feat(auth): add extractUserId to JwtPort and JwtAdapter"
```

---

### Task 2: Create `AuthenticatedUser` principal

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthenticatedUser.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.foodstock.auth.adapter.`in`

import java.util.UUID

data class AuthenticatedUser(val userId: UUID, val email: String)
```

- [ ] **Step 2: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/AuthenticatedUser.kt
git commit -m "feat(auth): add AuthenticatedUser principal data class"
```

---

### Task 3: Create `JwtAuthenticationFilter` with tests

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilter.kt`
- Create: `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilterTest.kt`

- [ ] **Step 1: Write failing tests**

Create `apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilterTest.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.domain.port.out.JwtPort
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    private val jwtPort: JwtPort = mock()
    private val filter = JwtAuthenticationFilter(jwtPort)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `sets authenticated principal when Bearer token is valid`() {
        val token = "valid.jwt.token"
        val userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val email = "user@example.com"
        whenever(jwtPort.validateToken(token)).thenReturn(true)
        whenever(jwtPort.extractEmail(token)).thenReturn(email)
        whenever(jwtPort.extractUserId(token)).thenReturn(userId)

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        val principal = auth.principal as AuthenticatedUser
        assertEquals(userId, principal.userId)
        assertEquals(email, principal.email)
    }

    @Test
    fun `does not set authentication when Authorization header is absent`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `does not set authentication when token is invalid`() {
        val token = "bad.token"
        whenever(jwtPort.validateToken(token)).thenReturn(false)

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `does not set authentication when Authorization header is not Bearer`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```
cd apps/api && ./gradlew test --tests "*.JwtAuthenticationFilterTest"
```

Expected: compile error — `JwtAuthenticationFilter` does not exist yet.

- [ ] **Step 3: Implement `JwtAuthenticationFilter`**

Create `apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilter.kt`:

```kotlin
package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.domain.port.out.JwtPort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(private val jwtPort: JwtPort) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ")
            if (jwtPort.validateToken(token)) {
                val principal = AuthenticatedUser(
                    userId = jwtPort.extractUserId(token),
                    email = jwtPort.extractEmail(token)
                )
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(principal, null, emptyList())
            }
        }
        filterChain.doFilter(request, response)
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```
cd apps/api && ./gradlew test --tests "*.JwtAuthenticationFilterTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilter.kt \
        apps/api/src/test/kotlin/com/foodstock/auth/adapter/in/JwtAuthenticationFilterTest.kt
git commit -m "feat(auth): add JwtAuthenticationFilter"
```

---

### Task 4: Register filter in `AuthConfig` and `SecurityConfig`

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/config/AuthConfig.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/auth/config/SecurityConfig.kt`

- [ ] **Step 1: Expose filter as a `@Bean` in `AuthConfig`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/auth/config/AuthConfig.kt`:

```kotlin
package com.foodstock.auth.config

import com.foodstock.auth.adapter.`in`.JwtAuthenticationFilter
import com.foodstock.auth.adapter.out.BcryptPasswordHashAdapter
import com.foodstock.auth.adapter.out.JwtAdapter
import com.foodstock.auth.adapter.out.UserJpaRepository
import com.foodstock.auth.domain.service.AuthService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AuthConfig(
    private val userJpaRepository: UserJpaRepository,
    private val passwordHashAdapter: BcryptPasswordHashAdapter,
    private val jwtAdapter: JwtAdapter
) {
    @Bean
    fun authService(): AuthService = AuthService(
        userRepository = userJpaRepository,
        passwordHashPort = passwordHashAdapter,
        jwtPort = jwtAdapter
    )

    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(jwtAdapter)
}
```

- [ ] **Step 2: Register filter in `SecurityConfig`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/auth/config/SecurityConfig.kt`:

```kotlin
package com.foodstock.auth.config

import com.foodstock.auth.adapter.`in`.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
```

- [ ] **Step 3: Run all tests to confirm nothing is broken**

```
cd apps/api && ./gradlew test
```

Expected: `BUILD SUCCESSFUL` — all existing tests pass because `@AutoConfigureMockMvc(addFilters = false)` skips the filter.

- [ ] **Step 4: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/auth/config/AuthConfig.kt \
        apps/api/src/main/kotlin/com/foodstock/auth/config/SecurityConfig.kt
git commit -m "feat(auth): register JwtAuthenticationFilter in security filter chain"
```

---

### Task 5: Create `HouseAccessDeniedException`

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/HouseAccessDeniedException.kt`

- [ ] **Step 1: Create the exception**

`HouseAccessDeniedException` extends `ForbiddenOperationException`, which is already handled by `GlobalExceptionHandler` returning HTTP 403. No handler changes needed.

```kotlin
package com.foodstock.inventory.domain.exception

import com.foodstock.common.exception.ForbiddenOperationException
import java.util.UUID

class HouseAccessDeniedException(houseId: UUID) :
    ForbiddenOperationException("User is not an active member of house: $houseId")
```

- [ ] **Step 2: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/exception/HouseAccessDeniedException.kt
git commit -m "feat(inventory): add HouseAccessDeniedException"
```

---

### Task 6: Create `HouseMemberCheckPort`

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/out/HouseMemberCheckPort.kt`

- [ ] **Step 1: Create the port**

```kotlin
package com.foodstock.inventory.domain.port.out

import java.util.UUID

interface HouseMemberCheckPort {
    fun isActiveMember(houseId: UUID, userId: UUID): Boolean
}
```

- [ ] **Step 2: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/out/HouseMemberCheckPort.kt
git commit -m "feat(inventory): add HouseMemberCheckPort"
```

---

### Task 7: Create `HouseMemberCheckAdapter`

**Files:**
- Create: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/out/HouseMemberCheckAdapter.kt`

Note: `adapter/out/**` is excluded from the JaCoCo coverage gate — no unit test needed here. The adapter is exercised in integration.

- [ ] **Step 1: Create the adapter**

```kotlin
package com.foodstock.inventory.adapter.out

import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.inventory.domain.port.out.HouseMemberCheckPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HouseMemberCheckAdapter(
    private val houseMemberRepository: HouseMemberRepository
) : HouseMemberCheckPort {

    override fun isActiveMember(houseId: UUID, userId: UUID): Boolean {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId) ?: return false
        return member.status == MemberStatus.ACTIVE
    }
}
```

- [ ] **Step 2: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/inventory/adapter/out/HouseMemberCheckAdapter.kt
git commit -m "feat(inventory): add HouseMemberCheckAdapter"
```

---

### Task 8: Update domain contracts, service, config, and service tests

All files in this task must be committed together to keep compilation valid — the command/interface changes break the service and controller until all are updated simultaneously.

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/AddItemUseCase.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/UpdateItemQuantityUseCase.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`

- [ ] **Step 1: Write new failing service tests**

Add these tests to the bottom of `InventoryServiceTest.kt` (keep all existing tests, they will be updated in Step 3 after the interface changes are in):

```kotlin
// Tests to add (will fail to compile until interfaces are updated in Step 2):

@Test
fun `addItem throws HouseAccessDeniedException when user is not a member`() {
    val userId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

    assertThrows<HouseAccessDeniedException> {
        service.addItem(AddItemCommand(
            houseId = houseId, userId = userId, name = "Arroz",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null
        ))
    }
}

@Test
fun `getInventory throws HouseAccessDeniedException when user is not a member`() {
    val userId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

    assertThrows<HouseAccessDeniedException> {
        service.getInventory(houseId, null, userId)
    }
}

@Test
fun `updateQuantity throws HouseAccessDeniedException when user is not a member`() {
    val itemId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val existing = InventoryItem(
        id = itemId, houseId = houseId, name = "Leite", category = Category.FOOD,
        quantityLevel = QuantityLevel.PLENTY, expiryDate = null, notes = null,
        createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
    )
    whenever(inventoryRepository.findById(itemId)).thenReturn(existing)
    whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

    assertThrows<HouseAccessDeniedException> {
        service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.RUNNING_OUT, userId = userId))
    }
}

@Test
fun `getInventoryItem throws HouseAccessDeniedException when user is not a member`() {
    val itemId = UUID.randomUUID()
    val userId = UUID.randomUUID()
    val houseId = UUID.randomUUID()
    val item = InventoryItem(
        id = itemId, houseId = houseId, name = "Arroz", category = Category.FOOD,
        quantityLevel = QuantityLevel.PLENTY, expiryDate = null, notes = null,
        createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
    )
    whenever(inventoryRepository.findById(itemId)).thenReturn(item)
    whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

    assertThrows<HouseAccessDeniedException> {
        service.getInventoryItem(itemId, userId)
    }
}
```

- [ ] **Step 2: Update commands and use case interfaces**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/AddItemUseCase.kt`:

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.time.LocalDate
import java.util.UUID

data class AddItemCommand(
    val houseId: UUID,
    val userId: UUID,
    val name: String,
    val category: Category,
    val quantityLevel: QuantityLevel,
    val expiryDate: LocalDate?,
    val notes: String?
)

interface AddItemUseCase {
    fun addItem(command: AddItemCommand): InventoryItem
}
```

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/UpdateItemQuantityUseCase.kt`:

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

data class UpdateItemQuantityCommand(
    val itemId: UUID,
    val quantityLevel: QuantityLevel,
    val userId: UUID
)

interface UpdateItemQuantityUseCase {
    fun updateQuantity(command: UpdateItemQuantityCommand): InventoryItem
}
```

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryUseCase.kt`:

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import java.util.UUID

interface GetInventoryUseCase {
    fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?, userId: UUID): List<InventoryItem>
}
```

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/GetInventoryItemUseCase.kt`:

```kotlin
package com.foodstock.inventory.domain.port.`in`

import com.foodstock.inventory.domain.model.InventoryItem
import java.util.UUID

interface GetInventoryItemUseCase {
    fun getInventoryItem(itemId: UUID, userId: UUID): InventoryItem
}
```

- [ ] **Step 3: Update `InventoryService`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt`:

```kotlin
package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.exception.HouseAccessDeniedException
import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import com.foodstock.inventory.domain.port.out.HouseMemberCheckPort
import com.foodstock.inventory.domain.port.out.InventoryRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val houseMemberCheckPort: HouseMemberCheckPort,
    private val clock: Clock
) : AddItemUseCase, UpdateItemQuantityUseCase, GetInventoryUseCase, GetInventoryItemUseCase {

    override fun addItem(command: AddItemCommand): InventoryItem {
        checkMembership(command.houseId, command.userId)
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
        checkMembership(item.houseId, command.userId)
        val updated = item.copy(
            quantityLevel = command.quantityLevel,
            updatedAt = LocalDateTime.now(clock)
        )
        return inventoryRepository.save(updated)
    }

    override fun getInventory(houseId: UUID, quantityLevel: QuantityLevel?, userId: UUID): List<InventoryItem> {
        checkMembership(houseId, userId)
        return if (quantityLevel != null)
            inventoryRepository.findAllByHouseIdAndQuantityLevel(houseId, quantityLevel)
        else
            inventoryRepository.findAllByHouseId(houseId)
    }

    override fun getInventoryItem(itemId: UUID, userId: UUID): InventoryItem {
        val item = inventoryRepository.findById(itemId) ?: throw ItemNotFoundException(itemId)
        checkMembership(item.houseId, userId)
        return item
    }

    private fun checkMembership(houseId: UUID, userId: UUID) {
        if (!houseMemberCheckPort.isActiveMember(houseId, userId)) {
            throw HouseAccessDeniedException(houseId)
        }
    }
}
```

- [ ] **Step 4: Update `InventoryConfig`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt`:

```kotlin
package com.foodstock.inventory.config

import com.foodstock.inventory.adapter.out.HouseMemberCheckAdapter
import com.foodstock.inventory.adapter.out.InventoryJpaRepository
import com.foodstock.inventory.domain.service.InventoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class InventoryConfig(
    private val inventoryJpaRepository: InventoryJpaRepository,
    private val houseMemberCheckAdapter: HouseMemberCheckAdapter,
    private val clock: Clock
) {
    @Bean
    fun inventoryService(): InventoryService = InventoryService(
        inventoryRepository = inventoryJpaRepository,
        houseMemberCheckPort = houseMemberCheckAdapter,
        clock = clock
    )
}
```

- [ ] **Step 5: Update `InventoryServiceTest` — full replacement**

Full replacement of `apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt`:

```kotlin
package com.foodstock.inventory.domain.service

import com.foodstock.inventory.domain.exception.HouseAccessDeniedException
import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.out.HouseMemberCheckPort
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryServiceTest {

    private val inventoryRepository: InventoryRepository = mock()
    private val houseMemberCheckPort: HouseMemberCheckPort = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = InventoryService(inventoryRepository, houseMemberCheckPort, fixedClock)

    @Test
    fun `addItem saves item with generated id and timestamps`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val command = AddItemCommand(
            houseId = houseId, userId = userId, name = "Arroz", category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY, expiryDate = LocalDate.of(2026, 12, 31), notes = null
        )
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(houseId, result.houseId)
        assertEquals("Arroz", result.name)
        assertEquals(Category.FOOD, result.category)
        assertEquals(QuantityLevel.PLENTY, result.quantityLevel)
        assertEquals(LocalDate.of(2026, 12, 31), result.expiryDate)
        assertNotNull(result.id)
        assertEquals(expectedNow, result.createdAt)
        assertEquals(expectedNow, result.updatedAt)
    }

    @Test
    fun `addItem saves item without optional fields`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val command = AddItemCommand(
            houseId = houseId, userId = userId, name = "Sabão", category = Category.CLEANING,
            quantityLevel = QuantityLevel.RUNNING_OUT, expiryDate = null, notes = null
        )
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.addItem(command)

        assertEquals(houseId, result.houseId)
        assertEquals("Sabão", result.name)
        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
        assertEquals(null, result.expiryDate)
        assertEquals(null, result.notes)
        assertNotNull(result.id)
    }

    @Test
    fun `addItem throws HouseAccessDeniedException when user is not a member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

        assertThrows<HouseAccessDeniedException> {
            service.addItem(AddItemCommand(
                houseId = houseId, userId = userId, name = "Arroz", category = Category.FOOD,
                quantityLevel = QuantityLevel.PLENTY, expiryDate = null, notes = null
            ))
        }
    }

    @Test
    fun `updateQuantity updates quantityLevel on existing item`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val pastInstant = Instant.parse("2025-01-01T00:00:00Z")
        val existing = InventoryItem(
            id = itemId, houseId = houseId, name = "Leite",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.ofInstant(pastInstant, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(pastInstant, ZoneOffset.UTC)
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(existing)
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)
        whenever(inventoryRepository.save(any())).thenAnswer { it.arguments[0] as InventoryItem }

        val result = service.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.RUNNING_OUT, userId = userId)
        )

        val expectedUpdatedAt = LocalDateTime.now(fixedClock)
        assertEquals(QuantityLevel.RUNNING_OUT, result.quantityLevel)
        assertEquals(itemId, result.id)

        val captor = argumentCaptor<InventoryItem>()
        verify(inventoryRepository).save(captor.capture())
        assertEquals(QuantityLevel.RUNNING_OUT, captor.firstValue.quantityLevel)
        assertEquals(expectedUpdatedAt, captor.firstValue.updatedAt)
        assertEquals(existing.createdAt, captor.firstValue.createdAt)
    }

    @Test
    fun `updateQuantity throws ItemNotFoundException when item not found`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(inventoryRepository.findById(itemId)).thenReturn(null)

        assertThrows<ItemNotFoundException> {
            service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.ENOUGH, userId = userId))
        }
    }

    @Test
    fun `updateQuantity throws HouseAccessDeniedException when user is not a member`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val existing = InventoryItem(
            id = itemId, houseId = houseId, name = "Leite", category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY, expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(existing)
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

        assertThrows<HouseAccessDeniedException> {
            service.updateQuantity(UpdateItemQuantityCommand(itemId = itemId, quantityLevel = QuantityLevel.RUNNING_OUT, userId = userId))
        }
    }

    @Test
    fun `getInventory returns all items when no filter provided`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val item = InventoryItem(
            id = UUID.randomUUID(), houseId = houseId, name = "Arroz",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
        )
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)
        whenever(inventoryRepository.findAllByHouseId(houseId)).thenReturn(listOf(item))

        val result = service.getInventory(houseId, null, userId)

        assertEquals(1, result.size)
        assertEquals(item.id, result[0].id)
    }

    @Test
    fun `getInventory filters by quantityLevel when provided`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val item = InventoryItem(
            id = UUID.randomUUID(), houseId = houseId, name = "Arroz",
            category = Category.FOOD, quantityLevel = QuantityLevel.RUNNING_OUT,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
        )
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)
        whenever(inventoryRepository.findAllByHouseIdAndQuantityLevel(houseId, QuantityLevel.RUNNING_OUT))
            .thenReturn(listOf(item))

        val result = service.getInventory(houseId, QuantityLevel.RUNNING_OUT, userId)

        assertEquals(1, result.size)
        assertEquals(QuantityLevel.RUNNING_OUT, result[0].quantityLevel)
    }

    @Test
    fun `getInventory throws HouseAccessDeniedException when user is not a member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

        assertThrows<HouseAccessDeniedException> {
            service.getInventory(houseId, null, userId)
        }
    }

    @Test
    fun `getInventoryItem returns item by id`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val item = InventoryItem(
            id = itemId, houseId = houseId, name = "Arroz",
            category = Category.FOOD, quantityLevel = QuantityLevel.PLENTY,
            expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(item)
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(true)

        val result = service.getInventoryItem(itemId, userId)

        assertEquals(itemId, result.id)
    }

    @Test
    fun `getInventoryItem throws ItemNotFoundException when item does not exist`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(inventoryRepository.findById(itemId)).thenReturn(null)

        assertThrows<ItemNotFoundException> { service.getInventoryItem(itemId, userId) }
    }

    @Test
    fun `getInventoryItem throws HouseAccessDeniedException when user is not a member`() {
        val itemId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val item = InventoryItem(
            id = itemId, houseId = houseId, name = "Arroz", category = Category.FOOD,
            quantityLevel = QuantityLevel.PLENTY, expiryDate = null, notes = null,
            createdAt = LocalDateTime.now(fixedClock), updatedAt = LocalDateTime.now(fixedClock)
        )
        whenever(inventoryRepository.findById(itemId)).thenReturn(item)
        whenever(houseMemberCheckPort.isActiveMember(houseId, userId)).thenReturn(false)

        assertThrows<HouseAccessDeniedException> {
            service.getInventoryItem(itemId, userId)
        }
    }
}
```

- [ ] **Step 6: Run service tests**

```
cd apps/api && ./gradlew test --tests "*.InventoryServiceTest"
```

Expected: `BUILD SUCCESSFUL` — all 12 tests pass.

- [ ] **Step 7: Commit all domain + service + config changes**

```
git add apps/api/src/main/kotlin/com/foodstock/inventory/domain/port/in/ \
        apps/api/src/main/kotlin/com/foodstock/inventory/domain/service/InventoryService.kt \
        apps/api/src/main/kotlin/com/foodstock/inventory/config/InventoryConfig.kt \
        apps/api/src/test/kotlin/com/foodstock/inventory/domain/service/InventoryServiceTest.kt
git commit -m "feat(inventory): add userId to commands, wire membership check in InventoryService"
```

---

### Task 9: Update `InventoryController` and its tests

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`

- [ ] **Step 1: Replace `InventoryController`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`

import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.InventoryItemResponse
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.adapter.`in`.dto.toResponse
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.`in`.AddItemCommand
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityCommand
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
class InventoryController(
    private val addItemUseCase: AddItemUseCase,
    private val updateItemQuantityUseCase: UpdateItemQuantityUseCase,
    private val getInventoryUseCase: GetInventoryUseCase,
    private val getInventoryItemUseCase: GetInventoryItemUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @Valid @RequestBody request: AddItemRequest,
        @RequestHeader("X-House-Id") houseId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): InventoryItemResponse {
        val item = addItemUseCase.addItem(
            AddItemCommand(
                houseId = houseId,
                userId = user.userId,
                name = request.name.trim(),
                category = request.category,
                quantityLevel = request.quantityLevel,
                expiryDate = request.expiryDate,
                notes = request.notes
            )
        )
        return item.toResponse()
    }

    @PatchMapping("/{itemId}/quantity")
    fun updateQuantity(
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: UpdateQuantityRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): InventoryItemResponse {
        return updateItemQuantityUseCase.updateQuantity(
            UpdateItemQuantityCommand(itemId = itemId, quantityLevel = request.quantityLevel, userId = user.userId)
        ).toResponse()
    }

    @GetMapping
    fun getInventory(
        @RequestHeader("X-House-Id") houseId: UUID,
        @RequestParam(required = false) quantityLevel: QuantityLevel?,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): List<InventoryItemResponse> =
        getInventoryUseCase.getInventory(houseId, quantityLevel, user.userId).map { it.toResponse() }

    @GetMapping("/{itemId}")
    fun getInventoryItem(
        @PathVariable itemId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): InventoryItemResponse =
        getInventoryItemUseCase.getInventoryItem(itemId, user.userId).toResponse()
}
```

- [ ] **Step 2: Replace `InventoryControllerTest`**

Full replacement of `apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt`:

```kotlin
package com.foodstock.inventory.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.inventory.adapter.`in`.dto.AddItemRequest
import com.foodstock.inventory.adapter.`in`.dto.UpdateQuantityRequest
import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.exception.ItemNotFoundException
import com.foodstock.inventory.domain.port.`in`.AddItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryItemUseCase
import com.foodstock.inventory.domain.port.`in`.GetInventoryUseCase
import com.foodstock.inventory.domain.port.`in`.UpdateItemQuantityUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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

    @MockBean
    private lateinit var getInventoryUseCase: GetInventoryUseCase

    @MockBean
    private lateinit var getInventoryItemUseCase: GetInventoryItemUseCase

    private val userId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
    private fun principal(id: UUID = userId) = authentication(
        UsernamePasswordAuthenticationToken(
            AuthenticatedUser(userId = id, email = "user@test.com"), null, emptyList()
        )
    )

    @Test
    fun `addItem returns created inventory item response`() {
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(addItemUseCase.addItem(any())).thenReturn(item)

        mockMvc.post("/api/v1/inventory") {
            with(principal())
            contentType = MediaType.APPLICATION_JSON
            header("X-House-Id", item.houseId.toString())
            content = objectMapper.writeValueAsString(
                AddItemRequest(
                    name = "Arroz",
                    category = Category.FOOD,
                    quantityLevel = QuantityLevel.PLENTY,
                    expiryDate = LocalDate.parse("2026-12-31"),
                    notes = "Pacote fechado"
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
            with(principal())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UpdateQuantityRequest(QuantityLevel.RUNNING_OUT))
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
            with(principal())
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
            with(principal())
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

    @Test
    fun `getInventory returns all items for house`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryUseCase.getInventory(any(), any(), any())).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory") {
            with(principal())
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(item.id.toString()) }
                jsonPath("$[0].houseId") { value(item.houseId.toString()) }
                jsonPath("$[0].quantityLevel") { value("PLENTY") }
            }
    }

    @Test
    fun `getInventory filters by quantityLevel`() {
        val houseId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val item = inventoryItem(quantityLevel = QuantityLevel.RUNNING_OUT)
        whenever(getInventoryUseCase.getInventory(any(), any(), any())).thenReturn(listOf(item))

        mockMvc.get("/api/v1/inventory?quantityLevel=RUNNING_OUT") {
            with(principal())
            header("X-House-Id", houseId.toString())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].quantityLevel") { value("RUNNING_OUT") }
            }
    }

    @Test
    fun `getInventory returns 400 for invalid quantityLevel value`() {
        mockMvc.get("/api/v1/inventory?quantityLevel=INVALID") {
            with(principal())
            header("X-House-Id", UUID.randomUUID().toString())
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventory returns 400 when X-House-Id header is missing`() {
        mockMvc.get("/api/v1/inventory") {
            with(principal())
        }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `getInventoryItem returns item by id`() {
        val itemId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val item = inventoryItem(id = itemId, quantityLevel = QuantityLevel.PLENTY)
        whenever(getInventoryItemUseCase.getInventoryItem(any(), any())).thenReturn(item)

        mockMvc.get("/api/v1/inventory/$itemId") {
            with(principal())
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(itemId.toString()) }
                jsonPath("$.name") { value("Arroz") }
                jsonPath("$.quantityLevel") { value("PLENTY") }
            }
    }

    @Test
    fun `getInventoryItem returns 404 when item does not exist`() {
        val itemId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        whenever(getInventoryItemUseCase.getInventoryItem(any(), any())).thenThrow(ItemNotFoundException(itemId))

        mockMvc.get("/api/v1/inventory/$itemId") {
            with(principal())
        }
            .andExpect { status { isNotFound() } }
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

- [ ] **Step 3: Run controller tests**

```
cd apps/api && ./gradlew test --tests "*.InventoryControllerTest"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/inventory/adapter/in/InventoryController.kt \
        apps/api/src/test/kotlin/com/foodstock/inventory/adapter/in/InventoryControllerTest.kt
git commit -m "feat(inventory): replace X-House-Id header stub with @AuthenticationPrincipal"
```

---

### Task 10: Update `HouseController` and its tests

**Files:**
- Modify: `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`
- Modify: `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`

- [ ] **Step 1: Replace `HouseController`**

Full replacement of `apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt`:

```kotlin
package com.foodstock.household.adapter.`in`

import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val respondToInvitationUseCase: RespondToInvitationUseCase,
    private val getMyHousesUseCase: GetMyHousesUseCase,
    private val getHouseUseCase: GetHouseUseCase,
    private val getHouseMembersUseCase: GetHouseMembersUseCase
) {

    @GetMapping
    fun getMyHouses(
        @AuthenticationPrincipal user: AuthenticatedUser
    ): List<HouseResponse> =
        getMyHousesUseCase.getMyHouses(user.userId).map { HouseResponse(it.id, it.name, it.ownerId) }

    @GetMapping("/{houseId}")
    fun getHouse(
        @PathVariable houseId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseResponse {
        val house = getHouseUseCase.getHouse(houseId, user.userId)
        return HouseResponse(house.id, house.name, house.ownerId)
    }

    @GetMapping("/{houseId}/members")
    fun getHouseMembers(
        @PathVariable houseId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): List<HouseMemberResponse> =
        getHouseMembersUseCase.getHouseMembers(houseId, user.userId)
            .map { HouseMemberResponse(it.id, it.houseId, it.userId, it.role, it.status) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseResponse {
        val house = createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = user.userId)
        )
        return HouseResponse(id = house.id, name = house.name, ownerId = house.ownerId)
    }

    @PostMapping("/{houseId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteMember(
        @PathVariable houseId: UUID,
        @RequestBody request: InviteMemberRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseMemberResponse {
        val member = inviteMemberUseCase.inviteMember(
            InviteMemberCommand(
                houseId = houseId,
                invitedUserId = request.userId,
                invitedByUserId = user.userId
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
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseMemberResponse {
        val member = respondToInvitationUseCase.respondToInvitation(
            RespondToInvitationCommand(
                houseId = houseId,
                memberId = memberId,
                respondingUserId = user.userId,
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

- [ ] **Step 2: Replace `HouseControllerTest`**

Full replacement of `apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt`:

```kotlin
package com.foodstock.household.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
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

    @MockBean
    private lateinit var respondToInvitationUseCase: RespondToInvitationUseCase

    @MockBean
    private lateinit var getMyHousesUseCase: GetMyHousesUseCase

    @MockBean
    private lateinit var getHouseUseCase: GetHouseUseCase

    @MockBean
    private lateinit var getHouseMembersUseCase: GetHouseMembersUseCase

    private fun principal(userId: UUID) = authentication(
        UsernamePasswordAuthenticationToken(
            AuthenticatedUser(userId = userId, email = "user@test.com"), null, emptyList()
        )
    )

    @Test
    fun `createHouse returns created house`() {
        val houseId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val ownerId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val now = LocalDateTime.parse("2026-06-05T12:00:00")
        whenever(createHouseUseCase.createHouse(any())).thenReturn(
            House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = now, updatedAt = now)
        )

        mockMvc.post("/api/v1/houses") {
            with(principal(ownerId))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateHouseRequest(name = "Casa"))
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
                id = memberId, houseId = houseId, userId = invitedUserId,
                role = MemberRole.MEMBER, status = MemberStatus.PENDING,
                createdAt = LocalDateTime.parse("2026-06-05T12:00:00")
            )
        )

        mockMvc.post("/api/v1/houses/$houseId/members") {
            with(principal(invitedByUserId))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest(userId = invitedUserId))
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
    fun `respondToInvitation returns 200 with updated member on ACCEPT`() {
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val now = LocalDateTime.parse("2026-06-06T12:00:00")
        whenever(respondToInvitationUseCase.respondToInvitation(any())).thenReturn(
            HouseMember(id = memberId, houseId = houseId, userId = userId, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE, createdAt = now)
        )

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            with(principal(userId))
            contentType = MediaType.APPLICATION_JSON
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
            with(principal(userId))
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `inviteMember returns 403 when caller is not the house owner`() {
        val houseId = UUID.fromString("66666666-6666-6666-6666-666666666666")
        val nonOwnerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val invitedUserId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(inviteMemberUseCase.inviteMember(any()))
            .thenThrow(UnauthorizedMemberOperationException("Only the house owner can invite members"))

        mockMvc.post("/api/v1/houses/$houseId/members") {
            with(principal(nonOwnerId))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(InviteMemberRequest(userId = invitedUserId))
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Only the house owner can invite members") }
            }
    }

    @Test
    fun `respondToInvitation returns 403 when caller is not the invited user`() {
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val wrongUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        whenever(respondToInvitationUseCase.respondToInvitation(any()))
            .thenThrow(UnauthorizedMemberOperationException("Only the invited user can accept or reject an invitation"))

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            with(principal(wrongUserId))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Only the invited user can accept or reject an invitation") }
            }
    }

    @Test
    fun `getMyHouses returns list of houses`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val now = LocalDateTime.parse("2026-06-07T10:00:00")
        whenever(getMyHousesUseCase.getMyHouses(userId)).thenReturn(
            listOf(House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now))
        )

        mockMvc.get("/api/v1/houses") {
            with(principal(userId))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(houseId.toString()) }
                jsonPath("$[0].name") { value("Casa") }
                jsonPath("$[0].ownerId") { value(userId.toString()) }
            }
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
            with(principal(userId))
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
            with(principal(userId))
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
            with(principal(userId))
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
            with(principal(userId))
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
            with(principal(userId))
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
            with(principal(userId))
        }
            .andExpect { status { isForbidden() } }
    }
}
```

- [ ] **Step 3: Run all tests**

```
cd apps/api && ./gradlew test
```

Expected: `BUILD SUCCESSFUL` — all tests pass.

- [ ] **Step 4: Commit**

```
git add apps/api/src/main/kotlin/com/foodstock/household/adapter/in/HouseController.kt \
        apps/api/src/test/kotlin/com/foodstock/household/adapter/in/HouseControllerTest.kt
git commit -m "feat(household): replace X-User-Id header stub with @AuthenticationPrincipal"
```

---

### Task 11: Final verification

- [ ] **Step 1: Run the full test suite with coverage check**

```
cd apps/api && ./gradlew check
```

Expected: `BUILD SUCCESSFUL` — all tests pass and JaCoCo coverage gate (≥ 80% on included layers) is satisfied.

- [ ] **Step 2: Verify no cross-layer imports crept in**

```
cd apps/api && grep -r "adapter.out\|JpaEntity\|JpaRepository" src/main/kotlin/com/foodstock/inventory/domain/
```

Expected: no output — domain layer stays clean.
