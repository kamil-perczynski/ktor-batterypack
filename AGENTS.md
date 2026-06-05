# AGENTS.md

## Monorepo Boundaries

Root `ktor-frame` is the Ktor application. It includes `ktor-batterypack-*` subprojects as shared libraries (all published to GitHub Packages):

| Project | Purpose |
|---------|---------|
| `ktor-batterypack-core` | Config loader, health, exceptions, DI lifecycle, multipart, Jackson, controller auto-registration |
| `ktor-batterypack-database` | Exposed + Hikari + monitored transactions |
| `ktor-batterypack-metrics` | Micrometer + Prometheus |
| `ktor-batterypack-redis` | Lettuce client |
| `ktor-batterypack-database-testing` | Testcontainers PostgreSQL helper |
| `ktor-batterypack-redis-testing` | Testcontainers Redis helper |
| `ktor-batterypack-gradle-plugin` | Included build; provides `ktor-batterypack` plugin (`dockerDist`, `bootstrapDockerfile`, `bootstrapDockerignore`) |

`assemble` depends on `dockerDist`, so `./gradlew build` always produces the Docker distribution.

## Architecture (root app)

**Ports & Adapters**:
- `controllers/` → HTTP routes (driving adapters), `@Singleton` implementing `KtorController` (interface from `ktor-batterypack-core`)
- `domain/` → Business logic, entities, repository ports (interfaces). NO external framework dependencies.
- `infra/` → Infrastructure implementations (driven adapters), e.g. `infra/persistence/*`
- `libs/` → Minimal local helpers; most shared code lives in `ktor-batterypack-core`

**Dependency Rule**: `controllers` → `domain` ← `infra`

## Development

### Prerequisites

```bash
# Start PostgreSQL and Redis (both required for app and integration tests)
docker-compose up -d
```

### Running

```bash
# Dev server with live reload
./gradlew run

# Full build (compile + test + dockerDist)
./gradlew build

# Docker distribution (separated app/dependency layers)
./gradlew dockerDist

# Bootstrap Dockerfile / .dockerignore
./gradlew bootstrapDockerfile bootstrapDockerignore
```

Server starts at `http://localhost:8080`.

### Toolchain Quirks

- **Java 25** toolchain in all modules; CI uses Temurin.
- **JEP 472 workaround**: `--enable-native-access=ALL-UNNAMED` is required for `JavaExec` and test JVMs because Netty loads native libraries from an unnamed module. Already present in every `build.gradle.kts`.

### Testing

```bash
# All tests
./gradlew test

# Single test class
./gradlew test --tests "io.github.kperczynski.infra.config.LoadConfigTest"
```

**Integration test base**: `KtorBatteriesIT` spins up shared Testcontainers **PostgreSQL + Redis** once per JVM. Tests inject Koin beans via `application.koin().get<...>()`.

## Configuration

Uses **Hoplite** with this precedence (highest first):
1. Environment variables (`UPPER_CASE_WITH_UNDERSCORES`)
2. System properties (`config.override.*`)
3. `application-{profile}.yaml` (profile-specific, reversed order for last-wins)
4. `application.yaml` (committed defaults)

`application-local.yaml` is gitignored. Example: `DATABASE_URL=jdbc:postgresql://...`

`APP_PROFILES` env var selects active profiles.

## DI Wiring

Koin uses **annotation-based** configuration. `KtorFrameApp.kt` wires explicit batterypack modules + `KtorFrameModule`:

```kotlin
@KoinApplication(
    modules = [
        KtorBatterypackCoreModule::class,
        KtorFrameModule::class,
        FlorinModule::class,
        KtorBatterypackDatabaseModule::class,
        KtorBatterypackMetricsModule::class,
        KtorBatterypackRedisModule::class
    ]
)
object KtorFrameApp

@Module
@ComponentScan("io.github.kperczynski")
@Configuration
class KtorFrameModule { ... }
```

- **Controllers auto-register**: `configureKtorServer` (from `ktor-batterypack-core`) discovers all `KtorController` beans and registers their routes.
- **Repositories**: annotate the implementation with `@Singleton` (no manual binding needed thanks to `@ComponentScan`).
- **Schema creation**: repositories implementing `InitCallback` run `onInit()` on startup via `KoinLifecycleListener`.
- **Cleanup**: any `@Singleton` implementing `AutoCloseable` is closed on shutdown.

## Adding Features

1. **Domain** (`domain/`): Define entity + repository interface (port) + service if needed
2. **Infra** (`infra/persistence/`): Implement repository using Exposed + `@Singleton` + `InitCallback` for schema creation
3. **Controller** (`controllers/`): Add HTTP routes as `@Singleton` implementing `KtorController`

No manual DI binding is required if the implementation is under `io.github.kperczynski` and annotated with `@Singleton`.

## Request Validation

Validation uses **Konform** and happens in controllers before service calls.

- **Validator class**: One per controller, as `@Singleton` bean in `controllers/` (e.g. `UserDtoValidator`)
- **ValidationException**: Thrown by validators, caught globally by `KtorExceptionHandler` (from `ktor-batterypack-core`)
- **Error response**: RFC 7807 `ProblemDetail` with HTTP 400, field errors in `extensionData`

## Framework Quirks

- **Exposed v1 API**: imports are `org.jetbrains.exposed.v1.*`, NOT the older `org.jetbrains.exposed.sql`.
- **Jackson 3**: imports are `tools.jackson.*`, NOT `com.fasterxml.jackson.*`.
- **Schema creation**: Repos implement `InitCallback` to auto-create tables on startup. No Flyway/manual migrations in dev.

## Key Files

| File | Purpose |
|------|---------|
| `KtorFrameApplicationServer.kt` | Root app wiring (`configureKtorServer`) |
| `infra/KtorFrameApp.kt` | Root Koin app + `KtorFrameModule` |
| `infra/KtorFrameProps.kt` | Application config aggregate (`AppProps`) |
| `main.kt` | Entry point (`EngineMain`) |
| `ktor-batterypack-core/.../KtorServerConfiguration.kt` | Auto-registers controllers, installs Koin/StatusPages/ContentNegotiation |
| `ktor-batterypack-core/.../KtorBatterypackCoreModule.kt` | Shared beans (Jackson, lifecycle, health) |
| `ktor-batterypack-core/.../config/ConfigLoader.kt` | Generic Hoplite config loader |
| `domain/*/*Repo.kt` | Repository PORT (interface) |
| `infra/persistence/*/*Repo.kt` | Repository implementation with `@Singleton` + `InitCallback` |

## CI / Deploy

- GitHub Actions runs `./gradlew build` on every PR/push to `main` with Java 25 (Temurin).
- On `main` merges, builds and pushes a multi-arch Docker image to `ghcr.io`.
- On tags, `./gradlew publish` pushes Maven packages to GitHub Packages (`ktor-batterypack-*`).

## Rules

1. Do NOT write any javadoc or comments in the codebase unless explicitly requested.
