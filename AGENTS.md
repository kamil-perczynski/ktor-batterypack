# AGENTS.md

## Architecture

This project follows **Ports & Adapters (Hexagonal) Architecture**:

```
controllers/   → HTTP routes (driving adapters)
domain/        → Business logic, entities, ports (interfaces)
infra/         → Infrastructure implementations (driven adapters)
libs/          → Shared utilities (lifecycle, health, exceptions, ktor helpers)
```

**Dependency Rule**: `controllers` → `domain` ← `infra`
- Controllers depend on domain ports (e.g., `UserRepository` interface)
- Infra implements domain ports (e.g., `ExposedUserRepository` implements `UserRepository`)
- Domain has NO external framework dependencies

## Development

### Prerequisites

```bash
# Start PostgreSQL (required for app and integration tests)
docker-compose up -d

# Verify database is ready
docker-compose exec postgres pg_isready -U ktor -d ktordb
```

### Running

```bash
# Run with live reload (Ktor plugin)
./gradlew run

# Run fat JAR
./gradlew runFatJar

# Build production JAR
./gradlew buildFatJar
```

Server starts at `http://localhost:8080`.

### Testing

```bash
# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "io.github.kperczynski.infra.config.LoadConfigTest"

# Run with info
./gradlew test --info
```

### Build

```bash
# Full build (compile + test)
./gradlew build

# Clean build
./gradlew clean build
```

## Configuration

Configuration uses **Hoplite** with this precedence (highest first):
1. Environment variables (`UPPER_CASE_WITH_UNDERSCORES`)
2. `application-local.yaml` (gitignored, for local dev)
3. `application.yaml` (committed defaults)

Example: `DATABASE_URL=jdbc:postgresql://...`

## DI Wiring

Koin uses **annotation-based** configuration in `infra/KtorFrameApp.kt`:

```kotlin
@KoinApplication
object KtorFrameApp

@Module(createdAtStart = true)
@ComponentScan("io.github.kperczynski")
class KtorFrameModule {
    @Singleton(binds = [UserRepository::class])
    fun userRepository(impl: ExposedUserRepository): UserRepository = impl
}
```

Controllers auto-register: any `@Singleton` implementing `KtorController` is discovered and registered. Repositories implementing `InitCallback` run schema creation on startup.

## Adding Features

1. **Domain** (`domain/`): Define entity + repository interface (port)
2. **Infra** (`infra/persistence/`): Implement repository using Exposed + `@Singleton`
3. **DI** (`infra/KtorFrameApp.kt`): Add binding for new repository
4. **Service** (`domain/`): Add domain service if business logic needed
5. **Controller** (`controllers/`): Add HTTP routes as `@Singleton` implementing `KtorController`

## Key Files

| File | Purpose |
|------|---------|
| `KtorFrameApplicationServer.kt` | Ktor app config, Koin init, routing, exception handling |
| `infra/KtorFrameApp.kt` | Koin DI module with `@ComponentScan` |
| `infra/KtorFrameProps.kt` | Hoplite config loading with env var support |
| `domain/*/UserRepository.kt` | Repository PORT (interface) |
| `infra/persistence/*Repository.kt` | Repository implementation with `@Singleton` |
| `main.kt` | Entry point (delegates to EngineMain) |

## Technologies

- **Framework**: Ktor 3.4 + Kotlin 2.3 + JVM 25
- **DI**: Koin 4.2 with annotations (`@KoinApplication`, `@Module`, `@Singleton`)
- **Database**: PostgreSQL + Exposed 1.2 (new v1 API) + HikariCP
- **Serialization**: Jackson + kotlinx.serialization
- **Config**: Hoplite (YAML + env vars)
- **Testing**: JUnit 5 + AssertJ

## Database

Exposed schema auto-creates on startup via `InitCallback`. No manual migrations needed for dev.

Default connection: `jdbc:postgresql://localhost:5432/ktordb` (user: `ktor` / `ktorpassword`)

**Important**: Exposed uses the new v1 API (`org.jetbrains.exposed.v1` package), not the older v0 API.

## Rules

1. Do NOT write any javadoc or comments in the codebase unless explicitly requested.