# Repository Guidelines

## Project Structure & Module Organization

This repository is an npm workspace monorepo with three apps and one shared package. `apps/api` contains the Kotlin/Spring Boot backend, organized by bounded context under `src/main/kotlin/com/foodstock`, with tests in `src/test/kotlin` and Flyway migrations in `src/main/resources/db/migration`. `apps/web` is the Vite React web client; app wiring lives in `src/app`, pages in `src/pages`, and feature code in `src/features`. `apps/mobile` is the Expo React Native client using Expo Router under `src/app`, screens under `src/screens`, and feature code under `src/features`. `packages/shared` exports shared TypeScript types, validators, and API client helpers.

## Build, Test, and Development Commands

- `npm run dev`: runs workspace development tasks through Turbo.
- `npm run build`: builds all packages/apps that define `build`.
- `npm run lint`: runs workspace lint tasks.
- `npm run test`: runs workspace test tasks where defined.
- `npm --workspace @food-stock/web run dev`: starts the web Vite dev server.
- `npm --workspace @food-stock/mobile run start`: starts Expo.
- `cd apps/api && .\gradlew test`: runs backend JUnit tests and generates JaCoCo reports.
- `docker compose -f infra/docker-compose.yml up`: starts local infrastructure when needed.

## Coding Style & Naming Conventions

Use TypeScript for web, mobile, and shared packages. Prefer React components in `PascalCase`, hooks named `useSomething`, and feature modules grouped by domain. Keep shared schemas and validators in `packages/shared/src` so both clients can reuse them. Kotlin code follows package-per-domain structure with `adapter`, `domain`, `port`, and `config` layers; keep controller, persistence, and domain service responsibilities separated.

## Testing Guidelines

Backend tests use JUnit Platform, Spring Boot Test, Mockito Kotlin, and JaCoCo. Name Kotlin tests after the unit under test, for example `InventoryServiceTest`. Add or update tests when changing domain services, ports, controllers, migrations, or security behavior. JaCoCo HTML/XML reports are produced after `gradlew test`; CI expects strong diff coverage even though the aggregate local threshold is currently set to 0%.

## Commit & Pull Request Guidelines

Recent history follows Conventional Commits: `feat(ci): ...`, `fix(inventory): ...`, and `chore(ci): ...`. Use a concise scope when useful, such as `inventory`, `shopping`, `api`, `web`, or `ci`. Pull requests should describe the change, list verification commands, link related issues, and include screenshots or screen recordings for visible web/mobile UI changes.

## Security & Configuration Tips

Do not commit secrets, tokens, or local database credentials. Keep API configuration in Spring `application.yml` or environment variables, and prefer Docker Compose for local services instead of hard-coded connection details.

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
