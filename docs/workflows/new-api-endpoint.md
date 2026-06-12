# Workflow: New API Endpoint

## Trigger
Use this workflow when you need to add a single new HTTP route inside an **existing** bounded context (`auth`, `household`, `inventory`, `shopping`). If you need a new context, use `docs/workflows/new-bounded-context.md` instead.

## Prerequisites
- `./gradlew build` passes locally
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)
- You know which bounded context owns this endpoint (e.g., `inventory`)
- You know the HTTP method and path (e.g., `GET /api/inventory/items/{id}`)
- You know the return type and any path/query parameters

## Steps

### 1. Add the use case interface to the in-port

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/in/<UseCase>.kt`:

```kotlin
package com.foodstock.<context>.domain.port.`in`

import com.foodstock.<context>.domain.model.<Entity>
import java.util.UUID

interface <UseCase> {
    fun execute(id: UUID): <Entity>
}
```

### 2. Write the service unit test (failing)

Add a test method to `apps/api/src/test/kotlin/com/foodstock/<context>/domain/service/<Context>ServiceTest.kt`:

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.kotlin.whenever
import java.util.UUID

@Test
fun `execute returns entity when found`() {
    val id = UUID.randomUUID()
    val expected = <Entity>(id = id)
    whenever(repository.findById(id)).thenReturn(expected)

    val result = service.execute(id)

    assertEquals(expected, result)
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest.execute returns entity when found"`
Expected: FAIL (method not found on service)

### 3. Implement the use case in the domain service

In `apps/api/src/main/kotlin/com/foodstock/<context>/domain/service/<Context>Service.kt`:

- Add `: <UseCase>` to the class implements list
- Implement the interface method:

```kotlin
override fun execute(id: UUID): <Entity> {
    return repository.findById(id)
        ?: throw <Context>NotFoundException(id)
}
```

If `<Context>NotFoundException` does not exist, create it in `domain/model/`:

```kotlin
package com.foodstock.<context>.domain.model

import java.util.UUID

class <Context>NotFoundException(id: UUID) :
    RuntimeException("Not found: $id")
```

Run: `./gradlew test --tests "*.<Context>ServiceTest"`
Expected: PASS

### 4. Add method to the out-port (only if the repository does not yet have it)

Check `apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/out/<Entity>Repository.kt`. If `findById` is missing, add it:

```kotlin
fun findById(id: UUID): <Entity>?
```

Then implement it in `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>RepositoryAdapter.kt`:

```kotlin
override fun findById(id: UUID): <Entity>? =
    jpa.findById(id).map { it.toDomain() }.orElse(null)
```

### 5. Wire the new use case into the config (only if a new interface was added)

In `apps/api/src/main/kotlin/com/foodstock/<context>/config/<Context>Config.kt`, ensure the service `@Bean` implements the new interface. If the service was already wired, no change is needed — Spring resolves it by type.

### 6. Write the controller slice test (failing)

Add a test to `apps/api/src/test/kotlin/com/foodstock/<context>/adapter/in/<Context>ControllerTest.kt`:

```kotlin
import com.foodstock.<context>.domain.model.<Entity>
import com.foodstock.<context>.domain.port.`in`.<UseCase>
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(<Context>Controller::class)
@AutoConfigureMockMvc(addFilters = false)
class <Context>ControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var useCase: <UseCase>

    @Test
    fun `GET api <context> id returns 200 with entity`() {
        val id = UUID.randomUUID()
        val entity = <Entity>(id = id)
        whenever(useCase.execute(id)).thenReturn(entity)

        mockMvc.get("/api/<context>/$id") {
            header("X-House-Id", "00000000-0000-0000-0000-000000000001")
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id.toString()) }
        }
    }
}
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: FAIL (handler method not found)

### 7. Add the handler method to the controller

In `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/in/<Context>Controller.kt`, add:

```kotlin
@GetMapping("/{id}")
fun get<Entity>(
    @PathVariable id: UUID,
    @RequestHeader("X-House-Id") houseId: UUID
): ResponseEntity<<Entity>Response> {
    val result = useCase.execute(id)
    return ResponseEntity.ok(<Entity>Response(id = result.id))
}
```

If a new response DTO is needed, define it in the same file:

```kotlin
data class <Entity>Response(val id: UUID)
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: PASS

### 8. Run the full test suite

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, no failures.

### 9. Commit

```bash
git add apps/api/src/main/kotlin/com/foodstock/<context>/
git add apps/api/src/test/kotlin/com/foodstock/<context>/
git commit -m "feat(<context>): add GET /api/<context>/{id} endpoint"
```

## Verification

- `./gradlew test` passes with no failures
- New use case interface exists in `domain/port/in/`
- Domain service implements the new interface (`: <UseCase>` in class declaration)
- Controller handler is annotated with the correct HTTP method annotation (`@GetMapping`, `@PostMapping`, etc.)
- `@MockBean` is used in the controller test — not `@Autowired` with the real implementation
- No JPA entity or Spring Data repository is imported in `domain/` packages
