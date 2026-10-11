---
name: kotlin-style-batterypack
description: Use when writing, reviewing, or refactoring any Kotlin code in the ktor-batterypack monorepo. Captures the house style — naming conventions, allowed API generations (Jackson 3, Exposed v1, Koin annotations), structure rules (no extension functions, block-syntax functions), testing style, and Gradle discipline. Keywords: Kotlin conventions, code style, naming, Jackson 3, Exposed, Koin, tests, Gradle.
---

# Kotlin Style — ktor-batterypack

Follow these conventions whenever writing Kotlin in this repo. They are mandatory unless the user explicitly overrides them.

## Hard rules

1. **Never write extension functions** unless the user explicitly requests one. Use plain member functions, top-level functions, or classes instead.
2. **Always use block syntax for functions** — every `fun` gets a `{ }` body. Never use expression-body syntax (`fun x(): Foo = ...`).
3. No inline `//` comments in code. KDoc (`/** ... */`) is allowed only on public APIs in the published `ktor-batterypack-*` libraries, never inside app/feature code (see AGENTS.md rule 1).

## Naming and structure

- One top-level class per file; the file name matches the class.
- Koin modules: `KtorBatterypack<Concern>Module` (e.g. `KtorBatterypackDatabaseModule`).
- Config data classes: `*Props` (e.g. `DatabaseProps`, `KtorProps`, `RedisProps`).
- Repository: interface `*Repo` in `domain/`, implementation `Exposed*Repo` in `infra/persistence/<entity>/` with a companion `*Table.kt` Exposed table.
- HTTP routes: `*Controller` implementing `ktor-batterypack-core`'s `KtorController`.
- Stream topics as top-level `const val` in the domain package (e.g. `PLANT_EVENTS_TOPIC`).
- One private logger per file: `private val log = LoggerFactory.getLogger(<Class>::class.java)`.
- Build with named arguments at call sites for anything beyond a couple of parameters.

## API generations (do not mix)

- **Jackson 3**: import from `tools.jackson.*`, never `com.fasterxml.*` from `com.fasterxml.jackson.annotation`.
- **Exposed v1**: imports from `org.jetbrains.exposed.v1.core.*` / `org.jetbrains.exposed.v1.jdbc.*`; run transactions with `suspendTransaction(db)`, never the old `transaction { }`.
- **DI**: Koin 4.2 annotations only — `@Module`, `@ComponentScan`, `@Singleton`, `@Singleton(binds = [Interface::class])`, `@Provided`, `@Named`, `@KoinApplication`. Use constructor injection; never `single { }` outside one-off root/config wiring.
- **Config**: Hoplite `loadConfig<T>(profiles)`; document every property with `@param:JsonPropertyDescription("...")`.
- **Validation**: `io.github.ktor_batterypack.validation` types (`ValidationResult`, `check()`, constraints) and `@Validator` / `@JsonValidator` annotated interfaces on the code.

## Idioms

- `suspend` for all I/O paths and repository methods; no blocking calls in suspend functions.
- Data classes with default values for DTOs and props.
- Fail fast: throw core domain exceptions (`ResourceMissingException` → 404, `ErrorCodeException` → 422, `IllegalArgumentException` → 400) instead of returning nulls or result wrappers.
- Explicit `@OptIn(...)` for experimental APIs — never silence with `@Suppress`.
- Registry-timed classes take `@Timed` on the implementation.
- Empty modules compose later: `@Module` + `@ComponentScan("<package>")` shells are fine for future concerns (e.g. kafka, authz).

## Testing

- JUnit 5 with backticked sentence test names: `fun `should create a user`()`.
- AssertJ for assertions (`assertThat`), `runTest` / `runBlocking` for suspend code.
- Integration tests: suffix `*IT`, run under the plain `test` task, share containers via a base class `KtorBatteriesIT` (Testcontainers PostgreSQL + Redis once per JVM); reach beans with `application.koin().get<T>()`.
- Reuse the `*-testing` modules (`database-testing`, `redis-testing`, `kafka-testing`) instead of hand-rolling containers.

## Gradle / toolchain

- Java 25 toolchain everywhere: `kotlin { jvmToolchain(25) }`.
- All dependency coordinates from version catalogs (`libs.*`, `ktorLibs.*`); no literal versions in module builds.
- Declare shared interfaces/types as `api(...)`; implementations as `implementation(...)`.
- Every JavaExec/test JVM needs the JEP 472 workaround: `jvmArgs("--enable-native-access=ALL-UNNAMED")` (Netty loads natives from an unnamed module).
- Test tasks: `useJUnitPlatform()` + full `testLogging` exception format.
