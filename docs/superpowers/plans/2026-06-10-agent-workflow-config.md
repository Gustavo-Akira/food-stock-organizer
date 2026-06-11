# Agent Workflow Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create four workflow doc files in `docs/workflows/` and enrich `CLAUDE.md` and `AGENTS.md` with a workflow index and explicit "never do" rules so AI agents can self-route to the correct step-by-step guide for any common task.

**Architecture:** Pure documentation — no source code changes. Each workflow file is a self-contained recipe (trigger → prerequisites → numbered steps → verification). `CLAUDE.md` and `AGENTS.md` each receive a new `## Workflows` section and sharpened constraint rules.

**Tech Stack:** Markdown only.

---

## File Map

| Action | Path |
|---|---|
| Create | `docs/workflows/new-bounded-context.md` |
| Create | `docs/workflows/new-api-endpoint.md` |
| Create | `docs/workflows/new-frontend-feature.md` |
| Create | `docs/workflows/database-schema-change.md` |
| Modify | `CLAUDE.md` (append `## Workflows` + `## Hard Rules`) |
| Modify | `AGENTS.md` (append `## Workflows` + `## Hard Rules`) |

---

## Task 1: Create docs/workflows/new-bounded-context.md

**Files:**
- Create: `docs/workflows/new-bounded-context.md`

- [ ] **Step 1: Create the file with the full content below**

Create `docs/workflows/new-bounded-context.md` with exactly this content:

```markdown
# Workflow: New Bounded Context

## Trigger
Use this workflow when you are asked to add a new domain area that does not fit any of the four existing contexts: `auth`, `household`, `inventory`, `shopping`.

## Prerequisites
- `./gradlew build` passes locally
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)
- You know the name of the new context (referred to as `<context>` below, e.g., `notifications`)
- You know the primary aggregate entity (referred to as `<Entity>` below, e.g., `Notification`)

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
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Clock

class <Context>ServiceTest {
    private val repository: <Entity>Repository = mock()
    private val clock: Clock = Clock.fixed(
        java.time.Instant.parse("2024-01-01T00:00:00Z"),
        java.time.ZoneOffset.UTC
    )
    private val service = <Context>Service(repository, clock)

    @Test
    fun `should do something`() {
        // arrange / act / assert
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

class <Context>Service(
    private val repository: <Entity>Repository,
    private val clock: Clock
) : Create<Entity> {
    override fun execute(/* params */): <Entity> {
        TODO("implement")
    }
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest"`
Expected: PASS

### 7. Implement the JPA entity

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

### 8. Implement the Spring Data JPA repository

Create `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>JpaRepository.kt`:

```kotlin
package com.foodstock.<context>.adapter.out

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface <Entity>JpaRepository : JpaRepository<<Entity>Entity, UUID>
```

### 9. Implement the repository adapter

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

### 10. Wire everything in the config

Create `apps/api/src/main/kotlin/com/foodstock/<context>/config/<Context>Config.kt`:

```kotlin
package com.foodstock.<context>.config

import com.foodstock.<context>.adapter.out.<Entity>JpaRepository
import com.foodstock.<context>.adapter.out.<Entity>RepositoryAdapter
import com.foodstock.<context>.domain.service.<Context>Service
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class <Context>Config {
    @Bean
    fun <entity>Repository(jpa: <Entity>JpaRepository): <Entity>RepositoryAdapter =
        <Entity>RepositoryAdapter(jpa)

    @Bean
    fun <context>Service(
        repository: <Entity>RepositoryAdapter,
        clock: Clock
    ): <Context>Service = <Context>Service(repository, clock)
}
```

### 11. Write the controller slice test (failing)

Create `apps/api/src/test/kotlin/com/foodstock/<context>/adapter/in/<Context>ControllerTest.kt`:

```kotlin
package com.foodstock.<context>.adapter.`in`

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.http.MediaType

@WebMvcTest(<Context>Controller::class)
class <Context>ControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Test
    fun `POST api <context> returns 200`() {
        mockMvc.post("/api/<context>") {
            contentType = MediaType.APPLICATION_JSON
            content = """{}"""
        }.andExpect { status { isOk() } }
    }
}
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: FAIL (class not found)

### 12. Implement the controller

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

### 13. Run full test suite

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, no failures.

### 14. Commit

```bash
git add apps/api/src/
git commit -m "feat(<context>): add <context> bounded context scaffold"
```

## Verification

- `./gradlew test` passes with no failures
- Package tree exists under `com/foodstock/<context>/`
- No Spring annotations (`@Service`, `@Component`) appear in `domain/`
- No JPA or Spring imports appear in `domain/model/` or `domain/service/`
```

- [ ] **Step 2: Verify file was created**

Run: `Test-Path docs/workflows/new-bounded-context.md` (PowerShell) or check the file exists.
Expected: file exists with sections `## Trigger`, `## Prerequisites`, `## Steps`, `## Verification`.

- [ ] **Step 3: Commit**

```bash
git add docs/workflows/new-bounded-context.md
git commit -m "docs(workflows): add new-bounded-context workflow guide"
```

---

## Task 2: Create docs/workflows/new-api-endpoint.md

**Files:**
- Create: `docs/workflows/new-api-endpoint.md`

- [ ] **Step 1: Create the file with the full content below**

Create `docs/workflows/new-api-endpoint.md` with exactly this content:

```markdown
# Workflow: New API Endpoint

## Trigger
Use this workflow when you need to add a single new HTTP route inside an **existing** bounded context (`auth`, `household`, `inventory`, `shopping`). If you need a new context, use `new-bounded-context.md` instead.

## Prerequisites
- `./gradlew build` passes locally
- You know which bounded context owns this endpoint (e.g., `inventory`)
- You know the HTTP method and path (e.g., `GET /api/inventory/items/{id}`)
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)

## Steps

### 1. Add the use case interface to the in-port

Create `apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/in/<UseCase>.kt`:

```kotlin
package com.foodstock.<context>.domain.port.`in`

interface <UseCase> {
    fun execute(/* input params */): <ReturnType>
}
```

### 2. Write the service unit test (failing)

Add a test method to `apps/api/src/test/kotlin/com/foodstock/<context>/domain/service/<Context>ServiceTest.kt`:

```kotlin
@Test
fun `<use case description>`() {
    // arrange
    val expected = /* expected return value */
    whenever(repository.<method>(<args>)).thenReturn(/* stub */)

    // act
    val result = service.<method>(<args>)

    // assert
    assertEquals(expected, result)
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest.<method name>"`
Expected: FAIL (method not found on service)

### 3. Implement the use case in the domain service

Add to `apps/api/src/main/kotlin/com/foodstock/<context>/domain/service/<Context>Service.kt`:

- Add `: <UseCase>` to the class implements list if not already there
- Implement the interface method:

```kotlin
override fun execute(/* params */): <ReturnType> {
    return repository.<method>(/* params */)
        ?: throw <Context>NotFoundException(/* id */)
}
```

Run: `./gradlew test --tests "*.<Context>ServiceTest"`
Expected: PASS

### 4. Add method to out-port (only if new DB access is needed)

If the service calls a repository method that does not exist yet, add it to:
`apps/api/src/main/kotlin/com/foodstock/<context>/domain/port/out/<Entity>Repository.kt`

```kotlin
fun <newMethod>(/* params */): <ReturnType>
```

Then implement it in `adapter/out/<Entity>RepositoryAdapter.kt`:

```kotlin
override fun <newMethod>(/* params */): <ReturnType> =
    jpa.<jpaMethod>(/* params */).map { it.toDomain() }.orElse(null)
```

### 5. Write the controller slice test (failing)

Add a test to `apps/api/src/test/kotlin/com/foodstock/<context>/adapter/in/<Context>ControllerTest.kt`:

```kotlin
@Test
fun `<HTTP method> /api/<context>/<path> returns <status>`() {
    val mockResult = /* mock return value */
    whenever(useCase.execute(<args>)).thenReturn(mockResult)

    mockMvc.<method>("/api/<context>/<path>") {
        // add headers, body as needed
        header("X-House-Id", "00000000-0000-0000-0000-000000000001")
    }.andExpect {
        status { is<Status>() }
        jsonPath("$.fieldName") { value("expectedValue") }
    }
}
```

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: FAIL (handler method not found)

### 6. Add the handler method to the controller

Add to `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/in/<Context>Controller.kt`:

```kotlin
@GetMapping("/{id}")   // adjust annotation and path
fun <handlerName>(
    @PathVariable id: UUID,
    @RequestHeader("X-House-Id") houseId: UUID
): ResponseEntity<<Entity>Response> {
    val result = useCase.execute(id)
    return ResponseEntity.ok(<Entity>Response(/* map */))
}
```

If a new DTO is needed, define it in the same file.

Run: `./gradlew test --tests "*.<Context>ControllerTest"`
Expected: PASS

### 7. Wire the new use case into the config (only if a new interface was added)

In `apps/api/src/main/kotlin/com/foodstock/<context>/config/<Context>Config.kt`, update the service `@Bean` to implement the new interface, or add a new `@Bean` if a separate class was created.

### 8. Run full test suite

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, no failures.

### 9. Commit

```bash
git add apps/api/src/
git commit -m "feat(<context>): add <HTTP method> <path> endpoint"
```

## Verification

- `./gradlew test` passes with no failures
- New interface exists in `domain/port/in/`
- Controller handler is annotated with the correct HTTP method annotation
- No domain imports appear in `adapter/out/` beyond the port interface
```

- [ ] **Step 2: Verify file was created**

File exists with sections `## Trigger`, `## Prerequisites`, `## Steps`, `## Verification`.

- [ ] **Step 3: Commit**

```bash
git add docs/workflows/new-api-endpoint.md
git commit -m "docs(workflows): add new-api-endpoint workflow guide"
```

---

## Task 3: Create docs/workflows/new-frontend-feature.md

**Files:**
- Create: `docs/workflows/new-frontend-feature.md`

- [ ] **Step 1: Create the file with the full content below**

Create `docs/workflows/new-frontend-feature.md` with exactly this content:

```markdown
# Workflow: New Frontend Feature

## Trigger
Use this workflow when adding a new user-facing feature to `apps/web` (React) or `apps/mobile` (Expo). This covers both new pages/screens and new sections within existing pages.

## Prerequisites
- The API endpoint this feature calls already exists and returns the expected shape
- `npm run build` passes from the repo root
- You know the target app: `web`, `mobile`, or both

## Steps

### 1. Add Zod schema and TypeScript types to packages/shared

In `packages/shared/src/`, create or extend the relevant schema file:

```typescript
// packages/shared/src/<domain>.ts
import { z } from 'zod'

export const <Entity>Schema = z.object({
  id: z.string().uuid(),
  // add fields matching the API response shape
})

export type <Entity> = z.infer<typeof <Entity>Schema>
```

Export from `packages/shared/src/index.ts`:

```typescript
export * from './<domain>'
```

### 2. Add API client helper to packages/shared

In `packages/shared/src/api/<domain>.ts`:

```typescript
import axios from 'axios'
import { <Entity>Schema } from '../<domain>'
import type { <Entity> } from '../<domain>'

export async function get<Entity>(id: string): Promise<<Entity>> {
  const res = await axios.get(`/api/<context>/${id}`)
  return <Entity>Schema.parse(res.data)
}
```

Export from `packages/shared/src/index.ts`.

### 3. Create the feature folder

For web: `apps/web/src/features/<featureName>/`
For mobile: `apps/mobile/src/features/<featureName>/`

Each feature folder contains only files for that feature. No imports from other feature folders.

### 4. Add a React Query hook

Create `apps/web/src/features/<featureName>/use<Entity>.ts` (or `apps/mobile/...`):

```typescript
import { useQuery } from '@tanstack/react-query'
import { get<Entity> } from '@food-stock/shared'

export function use<Entity>(id: string) {
  return useQuery({
    queryKey: ['<entity>', id],
    queryFn: () => get<Entity>(id),
    enabled: !!id,
  })
}
```

For mutations:

```typescript
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { create<Entity> } from '@food-stock/shared'

export function useCreate<Entity>() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: create<Entity>,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['<entity>'] })
    },
  })
}
```

### 5. Build the UI component

Create `apps/web/src/features/<featureName>/<Component>.tsx` (or `apps/mobile/...`):

```typescript
import { use<Entity> } from './use<Entity>'

export function <Component>({ id }: { id: string }) {
  const { data, isLoading, error } = use<Entity>(id)

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Error loading data</div>

  return (
    <div>
      {/* render data fields */}
    </div>
  )
}
```

For mobile, replace `<div>` with React Native primitives (`View`, `Text`, etc.).

### 6. Register the route

**Web** — add to `apps/web/src/app/router.tsx` (or equivalent routing file):

```typescript
import { <Page> } from '../pages/<PageName>'

// inside the routes array:
{ path: '/<path>', element: <<Page> /> }
```

**Mobile** — create `apps/mobile/src/app/<path>.tsx` (expo-router uses file-based routing):

```typescript
import { <Screen> } from '../../screens/<ScreenName>'

export default function <Path>Screen() {
  return <<Screen> />
}
```

### 7. Verify build passes

Run from repo root: `npm run build`
Expected: BUILD SUCCESSFUL for all packages

## Verification

- `npm run build` passes from repo root
- Feature folder exists under `features/<featureName>/`
- No imports cross feature boundaries (no `../otherFeature/`)
- Zod schema and TypeScript type are exported from `packages/shared/src/index.ts`
- React Query key is a string array starting with the entity name
```

- [ ] **Step 2: Verify file was created**

File exists with sections `## Trigger`, `## Prerequisites`, `## Steps`, `## Verification`.

- [ ] **Step 3: Commit**

```bash
git add docs/workflows/new-frontend-feature.md
git commit -m "docs(workflows): add new-frontend-feature workflow guide"
```

---

## Task 4: Create docs/workflows/database-schema-change.md

**Files:**
- Create: `docs/workflows/database-schema-change.md`

- [ ] **Step 1: Create the file with the full content below**

Create `docs/workflows/database-schema-change.md` with exactly this content:

```markdown
# Workflow: Database Schema Change

## Trigger
Use this workflow whenever you need to modify the database schema: add a table, add/remove/rename a column, add an index, add a constraint.

## Prerequisites
- `./gradlew build` passes locally
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)
- You know what the schema change is (table name, column name, type, nullable)

## Rules
- **Never modify an existing migration file.** Flyway tracks applied migrations by checksum — modifying a file that was already applied will break the schema validator.
- Hibernate is set to `ddl-auto=validate` — it never creates or alters tables. Flyway is the only way to change the schema.

## Steps

### 1. Find the next migration version number

List files in `apps/api/src/main/resources/db/migration/`:

```
V1__create_initial_schema.sql
V2__...  ← if it exists
```

Use the next integer. If the latest is `V1`, your file is `V2__<description>.sql`.

### 2. Create the new migration file

Create `apps/api/src/main/resources/db/migration/V{N}__<description>.sql`:

**Add a column:**
```sql
ALTER TABLE <table_name> ADD COLUMN <column_name> <TYPE> [NOT NULL] [DEFAULT <value>];
```

**Add a table:**
```sql
CREATE TABLE <table_name> (
    id UUID PRIMARY KEY,
    <column> <TYPE> NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

**Add an index:**
```sql
CREATE INDEX idx_<table>_<column> ON <table_name>(<column_name>);
```

**Rename a column:**
```sql
ALTER TABLE <table_name> RENAME COLUMN <old_name> TO <new_name>;
```

### 3. Update the JPA entity

In `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>Entity.kt`, add or update the field:

```kotlin
@Column(name = "<column_name>", nullable = false)
val <fieldName>: <Type>,
```

For new tables, create a new `<Entity>Entity.kt` following the pattern in Task 1 of `new-bounded-context.md`.

### 4. Update toDomain() and fromDomain()

If the new column maps to a domain model field, update both mapping methods in the entity:

```kotlin
fun toDomain(): <Entity> = <Entity>(
    id = id,
    <fieldName> = <fieldName>,  // add this line
)

companion object {
    fun fromDomain(domain: <Entity>): <Entity>Entity = <Entity>Entity(
        id = domain.id,
        <fieldName> = domain.<fieldName>,  // add this line
    )
}
```

### 5. Update the domain model (if the field is domain-relevant)

In `apps/api/src/main/kotlin/com/foodstock/<context>/domain/model/<Entity>.kt`:

```kotlin
data class <Entity>(
    val id: UUID,
    val <fieldName>: <Type>,  // add this line
)
```

### 6. Update test fixtures

If any existing tests construct domain objects or entities directly, add the new field to those constructors. Search for usages:

```
./gradlew test 2>&1 | grep "error:"
```

Fix any compilation errors before proceeding.

### 7. Run build to validate schema alignment

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

Flyway applies the new migration on startup. If the SQL is invalid, the build fails with a Flyway exception. If the entity fields don't match the table columns, Hibernate's `ddl-auto=validate` throws a schema validation exception.

### 8. Commit migration and code changes together

```bash
git add apps/api/src/main/resources/db/migration/V{N}__<description>.sql
git add apps/api/src/main/kotlin/
git add apps/api/src/test/kotlin/
git commit -m "feat(<context>): add <column/table description> migration"
```

## Verification

- `./gradlew build` passes with no Flyway or Hibernate validation errors
- New migration file exists under `db/migration/` with the correct `V{N}__` prefix
- No existing migration file was modified (check `git diff HEAD~1 -- "*.sql"`)
- Entity fields match the new column definition
```

- [ ] **Step 2: Verify file was created**

File exists with sections `## Trigger`, `## Prerequisites`, `## Rules`, `## Steps`, `## Verification`.

- [ ] **Step 3: Commit**

```bash
git add docs/workflows/database-schema-change.md
git commit -m "docs(workflows): add database-schema-change workflow guide"
```

---

## Task 5: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Append the Workflows section**

At the end of `CLAUDE.md`, append:

```markdown
## Workflows

When executing a task, pick the matching workflow and follow it step by step.

| Task | Workflow |
|---|---|
| Add a new domain (bounded context) | `docs/workflows/new-bounded-context.md` |
| Add an endpoint to an existing context | `docs/workflows/new-api-endpoint.md` |
| Add a web or mobile feature | `docs/workflows/new-frontend-feature.md` |
| Change the database schema | `docs/workflows/database-schema-change.md` |

## Hard Rules

Violations of these rules break the hexagonal architecture or CI gate. Never do any of the following:

- **Never** call `LocalDateTime.now()` or `Instant.now()` directly in domain service code. Accept `Clock` as a constructor parameter and call `clock.instant()`.
- **Never** annotate a domain service class with `@Service`, `@Component`, `@Repository`, or any Spring stereotype. Wire it via `@Bean` in `{Context}Config.kt`.
- **Never** import a JPA entity (`*Entity`) or a Spring Data repository (`*JpaRepository`) from domain layer packages (`domain/model/`, `domain/service/`, `domain/port/`).
- **Never** modify an existing Flyway migration file (`V{N}__*.sql`). Always create a new versioned file.
- **Never** push code without tests for new endpoints or domain service methods. CI enforces ≥ 80% diff coverage.
- **Never** import from another feature folder on the frontend (`../otherFeature/`). Features are isolated.
```

- [ ] **Step 2: Verify the section was added**

Open `CLAUDE.md` and confirm `## Workflows` and `## Hard Rules` sections exist at the bottom.

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs(claude): add workflow index and hard rules"
```

---

## Task 6: Update AGENTS.md

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: Append the Workflows section**

At the end of `AGENTS.md`, append:

```markdown
## Workflows

When executing a task, pick the matching workflow and follow it step by step.

| Task | Workflow |
|---|---|
| Add a new domain (bounded context) | `docs/workflows/new-bounded-context.md` |
| Add an endpoint to an existing context | `docs/workflows/new-api-endpoint.md` |
| Add a web or mobile feature | `docs/workflows/new-frontend-feature.md` |
| Change the database schema | `docs/workflows/database-schema-change.md` |

## Hard Rules

Violations of these rules break the hexagonal architecture or CI gate. Never do any of the following:

- **Never** call `LocalDateTime.now()` or `Instant.now()` directly in domain service code. Accept `Clock` as a constructor parameter and call `clock.instant()`.
- **Never** annotate a domain service class with `@Service`, `@Component`, `@Repository`, or any Spring stereotype. Wire it via `@Bean` in `{Context}Config.kt`.
- **Never** import a JPA entity (`*Entity`) or a Spring Data repository (`*JpaRepository`) from domain layer packages (`domain/model/`, `domain/service/`, `domain/port/`).
- **Never** modify an existing Flyway migration file (`V{N}__*.sql`). Always create a new versioned file.
- **Never** push code without tests for new endpoints or domain service methods. CI enforces ≥ 80% diff coverage.
- **Never** import from another feature folder on the frontend (`../otherFeature/`). Features are isolated.
```

- [ ] **Step 2: Verify the section was added**

Open `AGENTS.md` and confirm `## Workflows` and `## Hard Rules` sections exist at the bottom.

- [ ] **Step 3: Commit**

```bash
git add AGENTS.md
git commit -m "docs(agents): add workflow index and hard rules"
```

---

## Final Verification

- [ ] Run: `ls docs/workflows/` — expected: four `.md` files
- [ ] Open `CLAUDE.md` — `## Workflows` and `## Hard Rules` sections present at the bottom
- [ ] Open `AGENTS.md` — same two sections present at the bottom
- [ ] All commits pushed: `git log --oneline -8`
