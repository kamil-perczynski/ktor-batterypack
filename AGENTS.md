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
- Controllers depend on domain ports (e.g., `UserRepo` interface)
- Infra implements domain ports (e.g., `ExposedUserRepo` implements `UserRepo`)
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

**Integration test base**: `KtorBatteriesIT` spins up a shared Testcontainers PostgreSQL instance once per JVM (port 5432 inside container, random host port). Tests inject Koin beans via `application.koin().get<...>()`.

### Build

```bash
# Full build (compile + test)
./gradlew build

# Clean build
./gradlew clean build

# Docker distribution (separated app/dependency layers, used by CI)
./gradlew dockerDist
```

## Configuration

Configuration uses **Hoplite** with this precedence (highest first):
1. Environment variables (`UPPER_CASE_WITH_UNDERSCORES`)
2. System properties (`config.override.*`)
3. `application-{profile}.yaml` (profile-specific, reversed order for last-wins)
4. `application.yaml` (committed defaults)

`application-local.yaml` is gitignored and intended for local overrides.
Example: `DATABASE_URL=jdbc:postgresql://...`

## DI Wiring

Koin uses **annotation-based** configuration. `KtorFrameApp.kt` defines explicit submodules and a component scan:

```kotlin
@KoinApplication(
    modules = [
        KtorFrameModule::class,
        FlorinModule::class,
        DatabaseModule::class,
        MetricsModule::class
    ]
)
object KtorFrameApp

@Module
@ComponentScan("io.github.kperczynski")
@Configuration
class KtorFrameModule { ... }
```

- **Controllers auto-register**: any `@Singleton` implementing `KtorController` is discovered and registered in `KtorFrameApplicationServer.kt`.
- **Repositories**: annotate the implementation with `@Singleton` (no manual binding needed thanks to `@ComponentScan`).
- **Schema creation**: repositories implementing `InitCallback` run `onInit()` on startup via `KoinLifecycleListener`.
- **Cleanup**: any `@Singleton` implementing `AutoCloseable` is closed on shutdown.

## Adding Features

1. **Domain** (`domain/`): Define entity + repository interface (port) + service if needed
2. **Infra** (`infra/persistence/`): Implement repository using Exposed + `@Singleton`
3. **Controller** (`controllers/`): Add HTTP routes as `@Singleton` implementing `KtorController`

No manual DI binding is required if the implementation is under `io.github.kperczynski` and annotated with `@Singleton`.

## Request Validation

Validation uses **Konform** library and happens in controllers before service calls.

- **Validator class**: One per controller, as `@Singleton` bean in `controllers/` (e.g. `UserDtoValidator`)
- **ValidationException**: Thrown by validators, caught globally by `KtorExceptionHandler`
- **Error response**: RFC 7807 `ProblemDetail` with HTTP 400, field errors in `extensionData`

Example validator:
```kotlin
@Singleton
class UserDtoValidator {
    fun validateCreate(value: UserCreate) {
        val result = createValidator(value)
        if (!result.isValid) {
            throw ValidationException(result.errors.map {
                FieldError(it.dataPath, it.message)
            })
        }
    }
}
```

Usage in controller:
```kotlin
post("/users") {
    val userCreate = call.receive<UserCreate>()
    userDtoValidator.validateCreate(userCreate)
    val createdUser = userService.create(userCreate)
    call.respond(HttpStatusCode.Created, createdUser)
}
```

## Key Files

| File | Purpose |
|------|---------|
| `KtorFrameApplicationServer.kt` | Ktor app config, Koin init, routing, exception handling |
| `infra/KtorFrameApp.kt` | Koin DI module with `@ComponentScan` and explicit submodules |
| `infra/KtorFrameProps.kt` | Hoplite config loading with env var support |
| `domain/*/*Repo.kt` | Repository PORT (interface) |
| `infra/persistence/*/*Repo.kt` | Repository implementation with `@Singleton` |
| `main.kt` | Entry point (delegates to `EngineMain`) |
| `libs/di/InitCallback.kt` | Startup initialization hook |
| `libs/ktor/KtorController.kt` | Controller interface for auto-registration |

## Technologies

- **Framework**: Ktor 3.4 + Kotlin 2.3 + JVM 25
- **DI**: Koin 4.2 with annotations (`@KoinApplication`, `@Module`, `@Singleton`)
- **Database**: PostgreSQL + Exposed 1.2 (new v1 API) + HikariCP
- **Serialization**: Jackson 3 + kotlinx.serialization
- **Config**: Hoplite (YAML + env vars)
- **Testing**: JUnit 5 + AssertJ + Mockito-Kotlin + Testcontainers
- **Metrics**: Micrometer + Prometheus registry

## Database

Exposed schema auto-creates on startup via `InitCallback`. No manual migrations needed for dev.

Default connection: `jdbc:postgresql://localhost:5432/ktordb` (user: `ktor` / `ktorpassword`)

**Important**: Exposed uses the **new v1 API** (`org.jetbrains.exposed.v1` package), not the older v0 API.

## CI / Deploy

GitHub Actions runs `./gradlew build` on every PR/push to `main` with Java 25 (Temurin).
On `main` branch merges, it also builds and pushes a multi-arch Docker image to `ghcr.io`.

## Rules

1. Do NOT write any javadoc or comments in the codebase unless explicitly requested.
