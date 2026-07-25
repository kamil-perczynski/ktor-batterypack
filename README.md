# ktor-batterypack

A **Ktor** backend application built with **Kotlin**, **Koin**, **Exposed**, and **PostgreSQL**. It ships with a modular `ktor-batterypack-*` monorepo of shared libraries that handle cross-cutting concerns—config, health, metrics, database transactions, Redis, and testcontainers helpers—so the root app stays focused on domain logic.

## What it does

The application exposes three domain areas over HTTP:

| Domain | Endpoints | Description |
|--------|-----------|-------------|
| **Users** | `POST /users`, `GET /users/{id}`, `PUT /users/{id}`, `DELETE /users/{id}` | CRUD with request validation and event publishing |
| **Plant Identification** | `GET /api/plants`, `GET /api/plants/{id}`, `POST /api/plant-identification` | Identify plants from uploaded images via an external Florin service |
| **Wallets** | `GET /api/wallets/{id}`, `GET /api/wallets/topup` | Wallet lookups and top-up requests with event-driven balance changes |

Every write operation publishes domain events (through Redis-backed event publishers) for downstream consumers.

## Architecture

**Ports & Adapters (Hexagonal)** in the root app:

```
controllers/   → HTTP routes (driving adapters), @Singleton implementing KtorController
domain/        → Business logic, entities, repository ports (interfaces). NO framework deps.
infra/         → Infrastructure implementations (driven adapters), e.g. infra/persistence/*
libs/          → Minimal local helpers; most shared code lives in ktor-batterypack-*
```

**Dependency rule**: `controllers` → `domain` ← `infra`

## Monorepo

Root `ktor-batterypack` embeds reusable `ktor-batterypack-*` libraries:

| Library | Purpose |
|---------|---------|
| `ktor-batterypack-core` | Config loader, health, exceptions, DI lifecycle, multipart, Jackson, controller auto-registration |
| `ktor-batterypack-database` | Exposed + Hikari + monitored transactions |
| `ktor-batterypack-metrics` | Micrometer + Prometheus |
| `ktor-batterypack-redis` | Lettuce client |
| `ktor-batterypack-database-testing` | Testcontainers PostgreSQL helper |
| `ktor-batterypack-redis-testing` | Testcontainers Redis helper |
| `ktor-batterypack-gradle-plugin` | Custom Gradle plugin (`dockerDist`, `bootstrapDockerfile`, `bootstrapDockerignore`) |

## Tech Stack

- **Runtime**: Ktor 3.4 + Kotlin 2.3 + JVM 25
- **DI**: Koin 4.2 with annotations
- **Database**: PostgreSQL + Exposed 1.2 (v1 API) + HikariCP
- **Cache/Events**: Redis (Lettuce)
- **Serialization**: Jackson 3 + kotlinx.serialization
- **Config**: Hoplite (YAML + env vars)
- **Testing**: JUnit 5 + AssertJ + Mockito-Kotlin + Testcontainers
- **Metrics**: Micrometer + Prometheus

## Prerequisites

- **JDK 25** (Temurin recommended)
- **Docker + Docker Compose** (for PostgreSQL, Redis, and integration tests)

## Quick Start

```bash
# 1. Start PostgreSQL and Redis
docker-compose up -d

# 2. Run the dev server
./gradlew run
```

Server starts at `http://localhost:8080`.

Default database connection: `jdbc:postgresql://localhost:5432/ktordb` (user: `ktor` / `ktorpassword`).

## Build & Package

```bash
# Full build (compile, test, dockerDist)
./gradlew build

# Docker distribution (separated app/dependency layers)
./gradlew dockerDist

# Bootstrap Dockerfile / .dockerignore
./gradlew bootstrapDockerfile bootstrapDockerignore
```

`assemble` depends on `dockerDist`, so `./gradlew build` always produces the Docker distribution.

## Testing

```bash
# All tests
./gradlew test

# Single test class
./gradlew test --tests "io.github.kperczynski.controllers.UserControllerIT"
```

**Integration tests** extend `KtorBatteriesIT`, which spins up shared Testcontainers **PostgreSQL + Redis** once per JVM. Beans are injected via `application.koin().get<...>()`.

## Configuration

Uses **Hoplite** with precedence (highest first):

1. Environment variables (`UPPER_CASE_WITH_UNDERSCORES`)
2. System properties (`config.override.*`)
3. `application-{profile}.yaml` (profile-specific, reversed order for last-wins)
4. `application.yaml` (committed defaults)

`application-local.yaml` is gitignored. Example: `DATABASE_URL=jdbc:postgresql://...`

Active profiles are selected via the `APP_PROFILES` environment variable.

## Adding a Feature

1. **Domain** (`domain/`): Define entity + repository interface (port) + service if needed
2. **Infra** (`infra/persistence/`): Implement repository using Exposed + `@Singleton` + `InitCallback` for schema creation
3. **Controller** (`controllers/`): Add HTTP routes as `@Singleton` implementing `KtorController`

No manual DI binding is required if the implementation is under `io.github.kperczynski` and annotated with `@Singleton`.

## Key Project Files

| File | Purpose |
|------|---------|
| `KtorFrameApplicationServer.kt` | Ktor app wiring (`configureKtorServer`) |
| `infra/KtorFrameApp.kt` | Root Koin application + module scan |
| `infra/KtorFrameProps.kt` | Aggregated application config (`AppProps`) |
| `main.kt` | Entry point (`EngineMain`) |

## CI / Deploy

- GitHub Actions runs `./gradlew build` on every PR/push to `main` with Java 25 (Temurin).
- On `main` merges, a multi-arch Docker image is built and pushed to `ghcr.io`.
- On tags, `./gradlew publish` pushes Maven packages to GitHub Packages (`ktor-batterypack-*`).
