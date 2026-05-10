# AGENTS.md

## Architecture

This project follows **Ports & Adapters (Hexagonal) Architecture**:

```
controllers/   → HTTP routes (driving adapters)
domain/        → Business logic, entities, ports (interfaces)
infra/         → Infrastructure implementations (driven adapters)
libs/          → Shared utilities (currently empty)
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
1. Environment variables (prefix: `APP_`)
2. `application-local.yaml` (gitignored, for local dev)
3. `application.yaml` (committed defaults)

Example: `APP_DATABASE_URL=jdbc:postgresql://...`

## DI Wiring

Koin binds interfaces to implementations in `infra/di/AppModule.kt`:

```kotlin
@Singleton(binds = [UserRepository::class])
fun userRepository(impl: ExposedUserRepository): UserRepository = impl
```

Controllers receive the interface, not the implementation.

## Adding Features

1. **Domain** (`domain/`): Define entity + repository interface port
2. **Infra** (`infra/persistence/`): Implement repository using Exposed
3. **DI** (`infra/di/AppModule.kt`): Add binding for new repository
4. **Service** (`domain/`): Add domain service if business logic needed
5. **Controller** (`controllers/`): Add HTTP routes

## Key Files

| File | Purpose |
|------|---------|
| `Exposed.kt` | Ktor app configuration, exception handling, routing setup |
| `infra/di/AppModule.kt` | Koin DI configuration |
| `domain/*/UserRepository.kt` | Repository PORT (interface) |
| `infra/persistence/*Repository.kt` | Repository implementation |
| `main.kt` | Entry point (delegates to EngineMain) |

## Technologies

- **Framework**: Ktor 3.4 + Kotlin 2.3 + JVM 21
- **DI**: Koin 4.2 with annotations
- **Database**: PostgreSQL + Exposed ORM + HikariCP
- **Serialization**: Jackson + kotlinx.serialization
- **Config**: Hoplite (YAML + env vars)
- **Testing**: JUnit 5 + AssertJ

## Database

Exposed schema auto-creates on startup via `InitCallback`. No manual migrations needed for dev.

Default connection: `jdbc:postgresql://localhost:5432/ktordb` (user: `ktor` / `ktorpassword`)
