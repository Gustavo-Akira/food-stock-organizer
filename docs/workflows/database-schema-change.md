# Workflow: Database Schema Change

## Trigger
Use this workflow whenever you need to modify the database schema: add a table, add/remove/rename a column, add an index, add a constraint.

## Prerequisites
- `./gradlew build` passes locally
- PostgreSQL is running (`docker compose -f infra/docker-compose.yml up -d`)
- You know what the schema change is (table name, column name, SQL type, nullable)

## Rules
- **Never modify an existing migration file.** Flyway tracks applied migrations by checksum — modifying a file that was already applied breaks the schema validator with a checksum mismatch error.
- **Never use `./gradlew bootRun` or Hibernate DDL to create tables.** `spring.jpa.hibernate.ddl-auto=validate` means Hibernate only validates; it never creates or alters tables.
- Flyway is the only valid way to change the database schema in this project.

## Steps

### 1. Find the next migration version number

List the files in `apps/api/src/main/resources/db/migration/`:

```
V1__create_initial_schema.sql
V2__...   ← if it exists
```

Use the next integer. If the highest existing version is `V1`, your file is named `V2__<description>.sql`.

### 2. Create the new migration file

Create `apps/api/src/main/resources/db/migration/V{N}__<description>.sql`. Use underscores in the description (e.g., `V2__add_expiry_date_to_inventory_items.sql`).

**Add a column:**
```sql
ALTER TABLE <table_name> ADD COLUMN <column_name> <TYPE> NOT NULL DEFAULT <value>;
-- Remove DEFAULT after backfill if the column should not have a default long-term:
-- ALTER TABLE <table_name> ALTER COLUMN <column_name> DROP DEFAULT;
```

**Add a new table:**
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

**Drop a column:**
```sql
ALTER TABLE <table_name> DROP COLUMN <column_name>;
```

### 3. Update the JPA entity

In `apps/api/src/main/kotlin/com/foodstock/<context>/adapter/out/<Entity>Entity.kt`, add or update the field to match the new column:

```kotlin
@Column(name = "<column_name>", nullable = false)
val <fieldName>: <KotlinType>,
```

For a nullable column, use:
```kotlin
@Column(name = "<column_name>", nullable = true)
val <fieldName>: <KotlinType>?,
```

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

### 5. Update the domain model (only if the field is domain-relevant)

In `apps/api/src/main/kotlin/com/foodstock/<context>/domain/model/<Entity>.kt`, add the field:

```kotlin
data class <Entity>(
    val id: UUID,
    val <fieldName>: <KotlinType>,  // add this line
)
```

Skip this step if the column is infrastructure-only (e.g., an audit timestamp that the domain never reasons about).

### 6. Update test fixtures

Search for all tests that construct `<Entity>` or `<Entity>Entity` directly and add the new field:

```bash
./gradlew test 2>&1 | grep -E "error:|unresolved"
```

Fix any compilation errors before proceeding. If a test creates a domain object without the new field, add a sensible default value for that test.

### 7. Run build to validate schema alignment

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

Flyway applies the new migration on startup. If the SQL is invalid, the build fails with a Flyway exception. If the entity fields do not match the table columns, Hibernate's `ddl-auto=validate` throws a `SchemaManagementException`.

### 8. Commit migration and code changes together

Always commit the migration file and the corresponding code changes in one commit so the repository never contains a state where the migration exists without the matching code (or vice versa):

```bash
git add apps/api/src/main/resources/db/migration/V{N}__<description>.sql
git add apps/api/src/main/kotlin/com/foodstock/<context>/
git add apps/api/src/test/kotlin/com/foodstock/<context>/
git commit -m "feat(<context>): <describe the schema change>"
```

## Verification

- `./gradlew build` passes with no Flyway or Hibernate validation errors
- New migration file exists in `db/migration/` with the correct `V{N}__` prefix
- No existing migration file was modified: `git diff HEAD~1 -- "*.sql"` shows only additions
- JPA entity fields match the new column definition (names, types, nullable)
- `toDomain()` and `fromDomain()` are updated if the domain model changed
