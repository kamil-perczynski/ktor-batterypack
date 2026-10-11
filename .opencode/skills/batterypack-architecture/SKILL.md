---
name: batterypack-architecture
description: Use when adding features, wiring beans, configuring startup, or touching framework-level code in the ktor-batterypack monorepo. Describes the stable high-level design — the boot sequence, the discovery-over-hardcoding principle, Koin composition stack, config precedence, error model, and the validation, persistence, events, and HTTP-client patterns. Keywords: architecture, composition, Koin, bootstrap, lifecycle, controllers, routes, health, ProblemDetail, validation.
---

# Batterypack Architecture

The stable framework design of the `ktor-batterypack-*` module family. Treat everything here as fixed; only feature wiring in `ktor-batterypack-example` is expected to change.

## Modules

| Module | Purpose |
|--------|---------|
| `ktor-batterypack-core` | Server bootstrap, contracts, config loader, health, exceptions, problem detail, JsonBinder, HTTP client factory, multipart |
| `ktor-batterypack-annotations` | `@Validator` / `@JsonValidator` markers used by the KSP codegen |
| `ktor-batterypack-validation` | Constraint model: `ValidationResult`, `ConstraintError` sealed tree, `Constraints` object |
| `ktor-batterypack-validation-ksp` | KSP processor generating `*Impl` validator classes from `@Validator` interfaces |
| `ktor-batterypack-database` | Hikari + Exposed wiring, Liquibase/Flyway migrations as bootstrap callbacks |
| `ktor-batterypack-metrics` | Micrometer/Prometheus registry, controllers, `@Timed` proxies |
| `ktor-batterypack-redis` | Lettuce client + Redis streams (publisher/listener/background loops) |
| `ktor-batterypack-kafka`, `ktor-batterypack-authz` | Empty `@Module` + `@ComponentScan` shells for future concerns |
| `ktor-batterypack-{database,redis,kafka}-testing` | Testcontainers helpers for tests |
| `ktor-batterypack-gradle-plugin` | `dockerDist`, `bootstrapDockerfile`, config-metadata generation |
| `ktor-batterypack-example` | Reference application (ports & adapters); its feature wiring MAY change |

## Boot sequence

```
main.kt (EngineMain)
  └─ Application.configureKtorServer(koinFn)
       ├─ resolveProfiles: app.profiles config → APP_PROFILES env → "local"
       ├─ subscribe KoinApplicationStarted  → LifecycleListener.onStart()
       ├─ subscribe KoinApplicationStopPreparing → LifecycleListener.onStop()
       ├─ install(Koin) { koinFn(ktorApp, koinApp, profiles) }
       │     └─ KoinLifecycleListener.onBootstrap()   ← runs while graph is built, before traffic
       └─ server accepts traffic → run InitCallbacks on start, close AutoCloseables on stop
```

The app-supplied `koinFn` receives `(Application, KoinApplication, profiles)` and must:
1. register `single { ktorApp }` so `@Provided Application` beans resolve,
2. register `single { loadConfig<ConfigMap>(profiles) }`,
3. install the `@KoinApplication` root via `koinApp.withConfiguration<...>()`.

## Discovery principle (the core design rule)

Components expose themselves as beans implementing contracts. The framework collects `List<Contract>` from the Koin graph — never hardcode lists of components.

| Contract (bean implements) | Collector | Effect |
|---|---|---|
| `KtorController` (`register(Routing)`) | `KtorControllersPlugin : KtorPlugin` | routes registered; also installs `CurrentApplicationCall` coroutine-context interceptor (`currentRoutingCall()`) |
| `KtorPlugin` (`register(Application)`) | `KtorPluginsBoostrapCallback` | app-level plugin installs (e.g. `ContentNegotiationKtorPlugin`) |
| `KtorExceptionHandler` (`register(StatusPagesConfig)`) | `KtorExceptionHandlerPlugin` | StatusPages mappings; `DefaultKtorExceptionHandler` provides defaults |
| `ReadinessCheck` (`suspend check(): HealthCheckResult`) | `ReadinessEndpoint` | `/actuator/health/readiness` (503 when down), `/actuator/health/liveness` and `/actuator/prometheus` from metrics module |
| `KtorHttpClientPlugin` | `KtorHttpClientFactory` | plugins installed on every created client |
| `MeterRegistryInit` | `PrometheusMeterRegistry` bean | inits ran before any meter is registered |
| `BootstrapCallback` (`onBootstrap()`) | `KoinLifecycleListener` | pre-start work (Liquibase, Flyway, plugin registration) |
| `InitCallback` (`onInit()`) | `KoinLifecycleListener` | post-start init; a failure fails startup |
| `AutoCloseable` | `KoinLifecycleListener` | closed on shutdown; close failures logged and swallowed |
| `RedisStreamListener` / `RedisStreamsBackgroundLoop` | `RedisStreamFetchers` | stream consumers; `RedisStreamFetchers` is a two-phase bean (`binds = [InitCallback::class, AutoCloseable::class]`) |
| `HikariCredentialsProvider` (optional) | `KtorBatterypackDatabaseModule` via `scope.getOrNull` | present→used, absent→ignored |

Consequences: prefer adding a bean over editing a registry; keep contracts one-method and in `ktor-batterypack-core` (or the owning module).

## Koin composition stack

- Root wiring: `@KoinApplication(modules = [ ... ])` object listing every battery module plus the app module; generated wrappers are activated with `koinApp.withConfiguration<Root>()`.
- Battery modules: `@Module` (+ `@Module(includes = [...])` to nest, e.g. streams include redis).
- App beans live under `@ComponentScan("io.github.kperczynski")` — annotate implementations with `@Singleton`; no manual binding needed.
- `@Provided` marks values constructed outside Koin (`Application`, `*Props`).
- `@Singleton(binds = [X::class])` for explicit interface binding; `@Named("florin")` / `@Named("oidc")` qualify multiple `HttpClient` instances.
- One `ConfigMap` aggregate from `loadConfig<ConfigMap>(profiles)`; the app module splits it into per-module `*Props` beans (`fun redisProps(configMap: ConfigMap): RedisProps { ... }`).

## Config

`loadConfig<T>(profiles)` precedence (highest first): env vars (`UPPER_CASE_WITH_UNDERSCORES`) → system properties (`config.override.*`) → `application-{profile}.yaml` (reversed order, last wins) → `application.yaml`. `APP_PROFILES` selects profiles; tests use `config.override.*` system properties. Every props field carries `@param:JsonPropertyDescription`; the gradle-plugin KSP step (`ktorBatterypack { configMetadataClass }`) generates config metadata.

## Error model (RFC 7807)

All responses are `ProblemDetail` (`type`, `title`, `status`, `detail`, `instance`, `extensionData`). `DefaultKtorExceptionHandler` maps:

| Exception | Status |
|---|---|
| `ValidationException` | 400 + `validation` extensionData |
| `IllegalArgumentException` | 400 |
| `ResourceMissingException` | 404 (entity_type/identifier extensionData) |
| `ErrorCodeException` (domain `ErrorCode`) | 422 |
| status 404 / 405 (no route/method) | 404 / 405 |
| `Throwable` | 500, logged with stacktrace |

Domain errors implement `ErrorCode` (`code`, `message`).

## Validation stack

1. Declare a `@Validator` (typed) or `@JsonValidator` (`JsonNode` in/out) interface with `fun validateX(...): ValidationResult<T>`.
2. KSP (`ktor-batterypack-validation-ksp`) generates `*Impl`; the interface exposes it through `companion object : X by XImpl()`.
3. Constraint sources: Jakarta validation annotations + optional YAML constraint dir (`kspArg("ktor.validation.constraint.dir", ...)`); runtime checks live in the `Constraints` object (`checkNotNull`, `checkPattern`, `checkSize`, ...).
4. Controllers bind inputs with `JsonBinder.bindBody<T>(call, validator::validateX)` / `bindQueryParams<T>(...)` — materialize as `JsonNode`, validate, then convert via the shared `JsonMapper`.
5. `result.check(message)` throws `ValidationException` → 400 problem detail.

## Persistence & events pattern

- Domain defines the `*Repo` interface (no framework deps); implementation is Exposed DAO (`*Table` objects + `*Entity`), each method inside `suspendTransaction(db)`; missing entity → `ResourceMissingException(Type::class.java, id)`; `find` methods throw, never return null (except explicit `findX(): T?`).
- Readiness: each dependency registers a `ReadinessCheck` bean (e.g. `DatabaseReadinessCheck`, `RedisReadinessCheck`).
- Events via Redis streams: topic constant + a thin `*EventPublisher` bean wrapping `RedisStreamPublisher`; a `*EventsListener : RedisStreamListener` with `config()` returning `RedisStreamListenerConfig(streamId = ...)`.
- Method-level timers: annotate interface-implementing beans with `@Timed` and call `enableTimedMethodsSampling(koinApp)` in the app's `configureKtorServer` callback.

## HTTP clients

`KtorHttpClientFactory` creates clients with: Java engine, Jackson 3 content negotiation, HEADERS-level logging with `Authorization` sanitized, connect/read timeouts, `expectSuccess = false`. Create one named client per downstream (`@Named("florin")`) from a `*ClientProps`; tag latency metrics with `pathPattern("/api/...")`.
