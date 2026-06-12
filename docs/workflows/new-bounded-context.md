# Workflow: New Bounded Context

## Trigger
Use this workflow when you are asked to add a new domain area that does not fit any of the four existing contexts: `auth`, `household`, `inventory`, `shopping`.

## Prerequisites
- `./gradlew build` passes locally
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)
- You know the name of the new context (referred to as `<context>` below, e.g., `notifications`)
- You know the primary aggregate entity (referred to as `<Entity>` below, e.g., `Notification`)
- You know the database table name (referred to as `<table_name>` below, e.g., `notification_items`). Convention: `<context>_items` for a single aggregate, `<context>_<entity>s` for multiple entities.

## Steps

### 1. Create the package tree

Create the following empty directories under `apps/api/src/main/kotlin/com/foodstock/<context>/`:

```
domain/model/
domain/service/
domain/port/in/
domain/port/out/
adapter/in/
adapter/out/
config/
```

And the mirror under `apps/api/src/test/kotlin/com/foodstock/<context>/`:

```
domain/service/
adapter/in/
```

### 2. Define the domain model

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/model/<Entity>.kt`:

```kotlin
package com.foodstock.<context>.domain.model

import java.util.UUID

data class <Entity>(
    val id: UUID,
    // add domain fields here — no Spring, JPA, or Jackson annotations
)
```

### 3. Define the in-port use case interface

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/in/<UseCase>.kt` for each use case. Example for a create use case:

```kotlin
package com.foodstock.<context>.domain.port.`in`

import com.foodstock.<context>.domain.model.<Entity>

interface Create<Entity> {
    fun execute(/* params */): <Entity>
}
```

### 4. Define the out-port repository interface

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/out/<Entity>Repository.kt`:

```kotlin
package com.foodstock.<context>.domain.port.out

import com.foodstock.<context>.domain.model.<Entity>
import java.util.UUID

interface <Entity>Repository {
    fun findById(id: UUID): <Entity>?
    fun save(entity: <Entity>): <Entity>
}
```

### 5. Write the service unit test (failing)

Create `apps/api/src/test/kotlin/com/foodstock/<context>/domain/service/<Context>ServiceTest.kt`:

```kotlin
package com.foodstock.<context>.domain.service

import com.foodstock.<context>.domain.port.out.<Entity>Repository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.util.UUID

class <Context>ServiceTest {
    private val repository: <Entity>Repository = mock()
    private val clock: Clock = Clock.fixed(
        java.time.Instant.parse("2024-01-01T00:00:00Z"),
        java.time.ZoneOffset.UTC
    )
    private val service = <Context>Service(repository, clock)

    @Test
    fun `should save and return entity`() {
        val entity = <Entity>(id = UUID.randomUUID())
        whenever(repository.save(any())).thenReturn(entity)
        val result = service.execute(entity.id)
        assertEquals(entity.id, result.id)
        verify(repository).save(any())
    }
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest"`
Expected: FAIL (class not found)

### 6. Implement the domain service

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/service/<Context>Service.kt`:

```kotlin
package com.foodstock.<context>.domain.service

import com.foodstock.<context>.domain.port.`in`.Create<Entity>
import com.foodstock.<context>.domain.port.out.<Entity>Repository
import java.time.Clock
import java.util.UUID

class <Context>Service(
    private val repository: <Entity>Repository,
    private val clock: Clock
) : Create<Entity> {
    override fun execute(id: UUID): <Entity> {
        return repository.findById(id) ?: throw NoSuchElementException("Not found: $id")
    }
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest"`
Expected: PASS

### 7. Create the Flyway migration

Before creating the JPA entity, create the migration file. Determine the next version number by listing `apps/api/src/main/resources/db/migration/`. Create `V{N}__create_<context>_schema.sql`:

```sql
CREATE TABLE <table_name> (
    id UUID PRIMARY KEY,
    -- add columns matching your domain model fields
    created_at TIMESTAMP NOT NULL
);
```

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL (Flyway applies the migration and Hibernate validates the schema)

### 8. Implement the JPA entity

Create `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>Entity.kt`:

```kotlin
package com.foodstock.<context>.adapter.out

import com.foodstock.<context>.domain.model.<Entity>
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "<table_name>")
class <Entity>Entity(
    @Id val id: UUID,
    // add columns here with JPA annotations
) {
    fun toDomain(): <Entity> = <Entity>(
        id = id,
        // map columns to domain fields
    )

    companion object {
        fun fromDomain(domain: <Entity>): <Entity>Entity = <Entity>Entity(
            id = domain.id,
            // map domain fields to columns
        )
    }
}
```

### 9. Implement the Spring Data JPA repository

Create `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>JpaRepository.kt`:

```kotlin
package com.foodstock.<context>.adapter.out

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface <Entity>JpaRepository : JpaRepository<<Entity>Entity, UUID>
```

### 10. Implement the repository adapter

Create `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>RepositoryAdapter.kt`:

```kotlin
package com.foodstock.<context>.adapter.out

import com.foodstock.<context>.domain.model.<Entity>
import com.foodstock.<context>.domain.port.out.<Entity>Repository
import java.util.UUID

class <Entity>RepositoryAdapter(
    private val jpa: <Entity>JpaRepository
) : <Entity>Repository {
    override fun findById(id: UUID): <Entity>? =
        jpa.findById(id).map { it.toDomain() }.orElse(null)

    override fun save(entity: <Entity>): <Entity> =
        jpa.save(<Entity>Entity.fromDomain(entity)).toDomain()
}
```

### 11. Wire everything in the config

Create `apps/api/src/main/kotlin/com/foodstock/<context>/config/<Context>Config.kt`:

```kotlin
package com.foodstock.<context>.config

import com.foodstock.<context>.adapter.out.<Entity>JpaRepository
import com.foodstock.<context>.adapter.out.<Entity>RepositoryAdapter
import com.foodstock.<context>.domain.port.out.<Entity>Repository
import com.foodstock.<context>.domain.service.<Context>Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class <Context>Config {
    @Bean
    fun <entity>Repository(jpa: <Entity>JpaRepository): <Entity>Repository =
        <Entity>RepositoryAdapter(jpa)

    @Bean
    fun <context>Service(
        repository: <Entity>Repository,
        clock: Clock
    ): <Context>Service = <Context>Service(repository, clock)
}
```

### 12. Write the controller slice test (failing)

Create `apps/api/src/test/kotlin/com/foodstock/<context>/adapter/in/<Context>ControllerTest.kt`:

> This project uses Spring Security on all routes except `/api/auth/**`. Add `@AutoConfigureMockMvc(addFilters = false)` to the test class to disable auth filters in the slice test:

```kotlin
package com.foodstock.<context>.adapter.`in`

import com.foodstock.<context>.domain.port.`in`.Create<Entity>
import java.util.UUID
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

@WebMvcTest(<Context>Controller::class)
@AutoConfigureMockMvc(addFilters = false)
class <Context>ControllerTest {
    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var create<Entity>: Create<Entity>

    @Test
    fun `POST api <context> returns 200`() {
        whenever(create<Entity>.execute(any())).thenReturn(<Entity>(id = UUID.randomUUID()))
        mockMvc.post("/api/<context>") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ /* fill with valid request fields */ }"""
        }.andExpect { status { isOk() } }
    }
}
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: FAIL (class not found)

### 13. Implement the controller

Create `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/in/<Context>Controller.kt`:

```kotlin
package com.foodstock.<context>.adapter.`in`

import com.foodstock.<context>.domain.port.`in`.Create<Entity>
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class Create<Entity>Request(/* request fields */)
data class <Entity>Response(/* response fields */)

@RestController
@RequestMapping("/api/<context>")
class <Context>Controller(
    private val create<Entity>: Create<Entity>
) {
    @PostMapping
    fun create(
        @RequestBody request: Create<Entity>Request
    ): ResponseEntity<<Entity>Response> {
        val result = create<Entity>.execute(/* map from request */)
        return ResponseEntity.ok(<Entity>Response(/* map from result */))
    }
}
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: PASS

### 14. Run full test suite

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, no failures.

### 15. Commit

```bash
git add apps/api/src/
git commit -m "feat(<context>): add <context> bounded context scaffold"
```

## Verification

- `./gradlew test` passes with no failures
- Package tree exists under `com/foodstock/<context>/`
- No Spring annotations (`@Service`, `@Component`) appear in `domain/`
- No JPA or Spring imports appear in `domain/model/` or `domain/service/`
