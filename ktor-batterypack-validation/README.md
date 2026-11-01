# ktor-batterypack-validation

Compile-time generated validators driven by Jakarta constraint annotations.
`ktor-batterypack-validation` is the runtime; `ktor-batterypack-validation-ksp` is the KSP processor that generates
the `*Impl` classes.

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.ktor.batterypack.validation)
    implementation(libs.jakarta.validation.api)
    ksp(libs.ktor.batterypack.validation.ksp)
}
```

## Validator modes

**Typed** validators check an already deserialized object:

```kotlin
@Validator
interface UserValidator {
    fun validate(user: UserCreate): ValidationResult<UserCreate>
}
```

**JSON** validators check a `JsonNode` before it is deserialized, so type mismatches, malformed formats and missing
properties are reported as validation errors instead of deserialization exceptions:

```kotlin
@JsonValidator
interface JsonUserValidator {
    @ValidationParamType(UserCreate::class)
    fun validate(user: JsonNode): ValidationResult<JsonNode>
}
```

## Which properties are validated

- Public properties that have a backing field, including properties inherited from superclasses.
  Computed properties (`val isAdult get() = age >= 18`) are skipped.
- Abstract properties declared by interfaces or abstract classes.
- Constraint annotations are collected from the property, its getter, its field and the matching primary-constructor
  parameter, so `@field:`, `@get:`, `@param:` and bare annotations all apply.
- In JSON mode, properties annotated with `@JsonIgnore` are skipped.

## Null semantics

| Declaration                                            | Typed mode         | JSON mode                         |
|--------------------------------------------------------|--------------------|-----------------------------------|
| `@NotNull` / `@NotBlank` / `@NotEmpty` on any property | `null` is rejected | `null` or missing is rejected     |
| `@NotNull` on the type (`val x: @NotNull String?`)     | `null` is rejected | `null` or missing is rejected     |
| Non-null type without a default (`val x: String`)      | not applicable     | `null` or missing is rejected     |
| Non-null type with a default (`val x: String = "a"`)   | not applicable     | missing is accepted               |
| Nullable type (`val x: String?`)                       | `null` is accepted | `null` or missing is accepted     |

- A rejected `null` is always reported as `{"constraint": "NotNull", "message": "Must not be null"}`, regardless of
  which annotation required the value.
- Every other constraint (`@Size`, `@Pattern`, `@Min`, ...) only runs when the value is present, so it accepts `null`.
- The same rules apply to collection items and map values: `List<String?>` accepts `null` items,
  `List<String>` and `List<@NotNull String?>` reject them.
- JSON mode does not reject an explicit `null` for a non-null property that has a default value. Configure Jackson
  (for example `KotlinFeature.NullIsSameAsDefault`) to decide how such payloads deserialize.

## JSON property names

JSON validators look up properties by their JSON name, and report errors under that name:

1. `@JsonProperty("first_name")` on the property, field, getter or constructor parameter wins.
2. Otherwise the KSP option `ktor.validation.json.naming` translates the Kotlin name.
3. Otherwise the Kotlin name is used.

```kotlin
ksp {
    arg("ktor.validation.json.naming", "SNAKE_CASE")
}
```

Supported values are the Jackson `PropertyNamingStrategies` constants: `LOWER_CAMEL_CASE`, `UPPER_CAMEL_CASE`,
`SNAKE_CASE`, `UPPER_SNAKE_CASE`, `LOWER_CASE`, `KEBAB_CASE` and `LOWER_DOT_CASE`. Any other value fails the build.
Use the same strategy as the `JsonMapper` that deserializes the payload.

Typed validators always report errors under the Kotlin property name.

## Error shape

```json
{
  "name": [{"constraint": "NotBlank"}],
  "address": {"zipCode": [{"constraint": "Pattern", "message": "Must match \\d{2}-\\d{3}"}]},
  "tags": {"errors": [{"constraint": "Size", "message": "Must have size between [1, 3]"}], "items": [null, {"$": [{"constraint": "NotBlank"}]}]},
  "labels": {"$": [{"constraint": "Size", "message": "Must have size between [0, 1]"}], "en": {"$": [{"constraint": "NotBlank"}]}}
}
```

- Scalar properties map to a list of errors.
- Nested objects map to an object keyed by property name.
- Lists map to `errors` (constraints on the list) and `items` (one entry per item, `null` when the item is valid).
- Constraints on a map or nested object, and errors on scalar list items or map values, are reported under `$`.

## Custom constraints

Point `ktor.validation.constraint.dir` at a directory of constraint descriptor YAML files
(schema: `constraints-descriptor.schema.yaml`):

```yaml
imports:
  - com.example.EvenConstraints
jsonImports:
  - com.example.EvenConstraints
defaults:
  simpleClassName: EvenConstraints
constraints:
  - annotation: com.example.Even
  - annotation: com.example.Present
    rejectsNull: true
```

By default a constraint is called as `EvenConstraints.checkEven(prop, value, call, <annotation args>)`, and only when
the value is present. Set `rejectsNull: true` to make the property required; a missing value is then reported as
`NotNull`.
