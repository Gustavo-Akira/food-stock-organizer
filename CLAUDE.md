# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Monorepo Structure

Turbo monorepo (`npm` workspaces) with four packages:

| Path | Stack | Purpose |
|---|---|---|
| `apps/api` | Kotlin 1.9 / Spring Boot 3.3 / JPA / PostgreSQL | REST API |
| `apps/web` | React 18 / Vite / TypeScript | Web frontend |
| `apps/mobile` | Expo 51 / React Native / expo-router | Mobile app |
| `packages/shared` | TypeScript / Zod | Shared API types & contracts |

## Commands

### Root (all apps via Turbo)
```
npm run build      # build everything
npm run dev        # dev servers for all apps
npm run test       # run all test suites
npm run lint       # lint all packages
```

### API (`apps/api`)
```
./gradlew test                        # run all tests + generate JaCoCo report
./gradlew test --tests "*.ClassName"  # run a single test class
./gradlew test --tests "*.ClassName.methodName"  # run a single test
./gradlew build                       # compile + test + check
./gradlew check                       # tests + JaCoCo coverage verification
./gradlew jacocoTestReport            # generate coverage HTML/XML reports
```

The API requires a running PostgreSQL instance. Environment variables (with defaults):
- `DATABASE_URL` → `jdbc:postgresql://localhost:5432/foodstock`
- `DATABASE_USERNAME` / `DATABASE_PASSWORD` → `foodstock`
- `JWT_SECRET` → change in production (min 32 chars)
- `API_PORT` → `8080`

## API Architecture

The API follows **hexagonal architecture** (ports & adapters), organized by bounded context under `com.foodstock`:

```
{context}/
  domain/
    model/          # pure Kotlin data classes — no framework annotations
    service/        # implements use cases; not @Service — wired manually
    port/
      in/           # use case interfaces (commands + interfaces)
      out/          # repository/external port interfaces
  adapter/
    in/             # @RestController + request/response DTOs
    out/            # JPA entities, Spring Data repositories, external adapters
  config/           # @Configuration that wires domain service beans
```

**Four bounded contexts:** `auth`, `household`, `inventory`, `shopping`.

### Wiring conventions

- Domain services have **no Spring annotations** (`@Service`, `@Component`). They are instantiated in `{Context}Config.kt` via `@Bean` methods, with all dependencies injected explicitly.
- `Clock` is a required constructor parameter on every domain service that needs time (never use `LocalDateTime.now()` directly in domain code). A single `Clock.systemUTC()` bean is defined in `InfrastructureConfig`.
- JPA entities live in `adapter/out` and expose `toDomain()` / `fromDomain(domain)` methods for mapping. They never leak into the domain layer.
- Request/response DTOs are defined in the same file as their `@RestController`.
- Use `@field:` prefix for Bean Validation annotations on Kotlin data class properties.

### Auth

JWT-based stateless auth. `/api/auth/**` is public; all other routes require a valid token. House ownership is currently enforced by passing `X-House-Id` as a request header — JWT claim extraction is a known TODO in `InventoryController`.

### CI Coverage Gate

CI runs on every push and PR via `.github/workflows/ci.yml`. The gate uses `madrapps/jacoco-report` to enforce **80% coverage on modified lines** (diff coverage). Overall project coverage threshold is intentionally 0% while the project matures. `./gradlew check` runs the same JaCoCo verification locally.

When adding new code, new lines must be covered at ≥ 80% or CI will fail on PRs targeting `main`.

## Database

Single Flyway migration (`V1__create_initial_schema.sql`) defines the full schema. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate never modifies the schema. All schema changes must be new versioned Flyway migration files (`V2__...`, etc.).

Tables mirror the bounded contexts: `users`, `houses`, `house_members`, `inventory_items`, `shopping_lists`, `shopping_list_items`.

## Frontend Patterns

Both `web` and `mobile` consume `packages/shared` for API types and Zod schemas. Key libraries:

- **Data fetching**: `@tanstack/react-query` + `axios`
- **State**: `zustand` (web only)
- **Validation**: `zod`
- **Routing**: `react-router-dom` (web), `expo-router` (mobile)

The PR checklist enforces **no cross-feature dependencies** in frontend code and **types updated in `packages/shared`** whenever API contracts change.

## PR Checklist

When opening PRs, follow `.github/pull_request_template.md`:
- Flyway migration created if schema changed
- `packages/shared` types updated if API contract changed
- Domain layer contains no infrastructure imports
- Build passes locally (`turbo build`)
