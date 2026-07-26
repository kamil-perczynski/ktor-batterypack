# Configuration Metadata Generation

## Problem

YAML configuration files (`application.yaml`, `application-{profile}.yaml`) used by Hoplite lack IDE autocompletion. There is no schema to validate against or provide property suggestions.

## Goal

Generate two files during the build, via a new Gradle task in `ktor-batterypack-gradle-plugin`:

1. **`META-INF/spring-configuration-metadata.json`** — Spring Boot configuration metadata format, recognized by IntelliJ/YAML language servers for autocompletion, type hints, and documentation.
2. **YAML schema file** — all properties with their default values, useful as a reference for users.

The target class is specified in the Gradle extension:

```kotlin
ktorBatterypack {
    configMetadataClass = "io.github.kperczynski.infra.AppProps"
}
```

## Phased Implementation

### Phase 1: Scaffolding & Minimal KSP Processor
**Goal**: Prove the plumbing works end-to-end

- Create `ktor-batterypack-ksp` subproject with basic structure
- Add to `settings.gradle.kts`
- Create a minimal `ConfigurationMetadataProcessor` that just logs "found class" when it runs
- Wire up in `ktor-batterypack-example` with KSP plugin
- Verify KSP runs successfully during build

**Deliverable**: KSP processor runs, logs output, no actual generation yet

### Phase 2: Basic Property Extraction (Flat Only)
**Goal**: Extract and output simple properties

- Implement class resolution via `Resolver.getClassDeclarationByName()`
- Extract primary constructor parameters (name, type)
- Generate basic `spring-configuration-metadata.json` with flat properties only
- Hardcode type mapping for primitives/String
- No nested classes, no defaults yet

**Deliverable**: JSON file generated with top-level properties from `AppProps`

### Phase 3: Unit Tests for Processor
**Goal**: Test coverage before adding complexity

- Set up KSP test infrastructure (ksp-testing, compile-testing)
- Write tests for class resolution, property extraction, JSON output
- Test with simple data classes (no nesting)

**Deliverable**: Test suite validating basic processor behavior

### Phase 4: Nested Data Class Support
**Goal**: Recursively process nested config classes

- Detect when a property type is a data class
- Recurse into nested classes with proper key prefixing
- Handle `KtorProps`, `DatabaseProps`, `RedisProps`, `FlorinClientProps`
- Update tests for nested scenarios

**Deliverable**: JSON includes all nested properties with correct dot-notation keys

### Phase 5: Type Mapping & Nullable Handling
**Goal**: Accurate type information in metadata

- Implement full type mapping table
- Handle nullable types
- Handle enum types (document as String with values)
- Update tests for type edge cases

**Deliverable**: Correct `type` field for all properties in JSON

### Phase 6: Default Value Extraction
**Goal**: Capture Kotlin default parameter values

- Extract default values from constructor parameters
- Handle primitives, String literals, null
- Handle complex defaults
- Update tests for default value scenarios

**Deliverable**: JSON includes `defaultValue` field where applicable

### Phase 7: YAML Schema Generation
**Goal**: Second output format

- Create YAML schema writer alongside JSON writer
- Generate `config-schema.yaml` with all properties and defaults
- Group by prefix for readability
- Add tests for YAML output

**Deliverable**: Both JSON and YAML files generated

### Phase 8: Gradle Plugin Integration
**Goal**: Proper task wiring in the plugin

- Add `configMetadataClass` property to `KtorBatterypackExtension`
- Register `generateConfigMetadata` task in `KtorBatterypackPlugin`
- Wire task to run as part of `./gradlew build`

**Deliverable**: `generateConfigMetadata` task runs as part of build

### Phase 9: End-to-End Integration & Polish
**Goal**: Full working example

- Configure `ktor-batterypack-example` with `configMetadataClass`
- Verify generated files appear in correct locations
- Test IntelliJ autocompletion works with generated metadata
- Add KDoc descriptions where available
- Final integration tests

**Deliverable**: Complete working feature with IDE autocompletion
