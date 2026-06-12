# Agent Workflow Configuration — Design Spec

**Date:** 2026-06-09
**Status:** Approved

## Goal

Provide AI agents (Claude, Gemini, Codex) with machine-readable, unambiguous step sequences for the four most common development tasks in this project. Also sharpen the existing `CLAUDE.md` and `AGENTS.md` with explicit "never do" rules and a workflow index so agents can self-route to the correct guide.

## Audience

AI agents running autonomously. Docs are optimized for machine parsing: numbered steps, exact file paths, exact commands, no explanatory prose.

## Files Modified

| File | Change |
|---|---|
| `CLAUDE.md` | Add `## Workflows` index + sharper "never do" rules |
| `AGENTS.md` | Add `## Workflows` index + sharper "never do" rules |

## Files Created

| File | Purpose |
|---|---|
| `docs/workflows/new-bounded-context.md` | Full recipe for adding a new domain context |
| `docs/workflows/new-api-endpoint.md` | Recipe for adding one endpoint inside an existing context |
| `docs/workflows/new-frontend-feature.md` | Recipe for adding a web or mobile feature |
| `docs/workflows/database-schema-change.md` | Recipe for safe schema changes via Flyway |

## Workflow File Template

Every workflow file follows this exact structure:

```
# Workflow: <Name>
## Trigger        — one-line condition for when to use this workflow
## Prerequisites  — what must already exist before starting
## Steps          — numbered steps, each naming exact file path or command
## Verification   — exact commands to confirm success before closing the task
```

## Content Per Workflow

### new-bounded-context.md

**Trigger:** A new domain concept does not fit any existing context (`auth`, `household`, `inventory`, `shopping`).

**Steps cover:**
1. Create package tree under `com.foodstock.<context>/`: `domain/model`, `domain/service`, `domain/port/in`, `domain/port/out`, `adapter/in`, `adapter/out`, `config`
2. Define domain model — pure Kotlin data class, zero Spring/JPA annotations
3. Define in-port use case interfaces
4. Define out-port repository interface
5. Implement JPA entity in `adapter/out` with `toDomain()` / `fromDomain()` methods
6. Implement Spring Data repository extending JPA entity repository
7. Implement domain service (no `@Service`) with `Clock` constructor parameter
8. Write `{Context}Config.kt` with `@Configuration` and explicit `@Bean` wiring
9. Add `@RestController` with request/response DTOs in the same file
10. Write service unit tests and controller slice tests
11. Run `./gradlew test`

### new-api-endpoint.md

**Trigger:** Adding one route to an existing bounded context controller.

**Steps cover:**
1. Add use case interface to `domain/port/in/`
2. Add method signature to domain service implementing the interface
3. If DB access needed: add method to out-port interface, implement in JPA adapter
4. Add handler method to `@RestController`; define request/response DTOs in same file
5. Wire new use case interface into `{Context}Config.kt` `@Bean` if needed
6. Write controller `@WebMvcTest` slice test for the new endpoint
7. Write domain service unit test for the new method
8. Run `./gradlew test --tests "*.{ServiceName}Test"` then `./gradlew test --tests "*.{ControllerName}Test"`

### new-frontend-feature.md

**Trigger:** Adding a new user-facing feature to `apps/web` or `apps/mobile`.

**Steps cover:**
1. Add Zod schema and TypeScript types to `packages/shared/src/`
2. Add API client helper function to `packages/shared/src/`
3. Create feature folder: `apps/web/src/features/<name>/` or `apps/mobile/src/features/<name>/`
4. Add React Query `useQuery` or `useMutation` hook inside the feature folder
5. Build UI component(s) — no cross-feature imports
6. Register route in `apps/web/src/app/` (react-router-dom) or `apps/mobile/src/app/` (expo-router)
7. Run `npm --workspace @food-stock/web run build` or `npm --workspace @food-stock/mobile run start`

### database-schema-change.md

**Trigger:** Any change to a database table: add/remove/rename column, add table, add index.

**Steps cover:**
1. Determine next migration version: inspect `apps/api/src/main/resources/db/migration/`, use next `V{N}`
2. Create `V{N}__<description>.sql` — never modify existing migration files
3. Update JPA entity fields in `adapter/out/` to match
4. Update `toDomain()` / `fromDomain()` mapping methods if domain model changes
5. Update domain model data class if the new field is domain-relevant
6. Run `./gradlew build` — Flyway validates schema; JPA `ddl-auto=validate` confirms alignment
7. If adding a field used in tests, update test fixtures

## CLAUDE.md / AGENTS.md Enrichments

### Workflows Index Section

New `## Workflows` section added to both files:

```markdown
## Workflows

When executing a task, pick the matching workflow doc and follow it step by step.

| Task | Workflow |
|---|---|
| Add a new domain (context) | `docs/workflows/new-bounded-context.md` |
| Add an endpoint to an existing context | `docs/workflows/new-api-endpoint.md` |
| Add a web or mobile feature | `docs/workflows/new-frontend-feature.md` |
| Change the database schema | `docs/workflows/database-schema-change.md` |
```

### Explicit "Never Do" Rules

Added to the API Architecture section of `CLAUDE.md` and Coding Style section of `AGENTS.md`:

- **Never** call `LocalDateTime.now()` or `Clock.systemUTC()` directly in domain service code — accept `Clock` as a constructor parameter and call `clock.instant()` or equivalent.
- **Never** annotate domain service classes with `@Service`, `@Component`, or any Spring annotation — wire them via `@Bean` in `{Context}Config.kt`.
- **Never** import JPA entities or Spring Data repositories from the domain layer — only the out-port interface may be referenced.
- **Never** modify an existing Flyway migration file — always create a new versioned file.
- **Never** skip writing tests for a new endpoint or domain service method — CI enforces 80% diff coverage.
- **Never** share state between features on the frontend — no cross-feature imports.

## Verification

After creating all files, an agent must confirm:
- `./gradlew build` passes (no compilation errors from any doc-referenced pattern)
- All four workflow files exist under `docs/workflows/`
- `CLAUDE.md` and `AGENTS.md` each contain a `## Workflows` section
