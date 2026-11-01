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

## Validator functions

Abstract functions in a validator interface must have one of two shapes:

- `fun validate(user: UserCreate): ValidationResult<UserCreate>` is generated as a public entry point. In JSON mode
  the parameter and result type are `JsonNode` and the model type comes from `@ValidationParamType`.
- `fun validateAddress(address: Address?, call: ValidationCall)` is generated as the validation of `Address`, replacing
  the private method that would otherwise be generated. It must be named `validate` + the type's simple name.

The type part of a validation function name is the simple name followed by the names of its type arguments, so
`List<Address>` is validated by `validateListAddress`. When one validator reaches different classes with the same
simple name, each one is prefixed with the shortest distinguishing package segments: `com.example.billing.Address`
and `com.example.shipping.Address` are validated by `validateBillingAddress` and `validateShippingAddress`.
Generated code imports these classes under aliases built the same way, such as `BillingAddress`.

Implementing `fun validateAddress(address: Address?, call: ValidationCall)` yourself replaces the generated
validation of `Address`. In JSON mode, annotate it with `@ValidationParamType(Address::class)` and accept an
`ObjectNode?`. Implemented functions of the same shape under another name run in addition to the generated
validation of their type. Other implemented functions are ignored, so interfaces can hold helpers of any shape.

## Supported types

- Objects are validated property by property, at any nesting depth. Types from `java.*`, `javax.*`, `jakarta.*`,
  `kotlin.*` and `kotlinx.*` are treated as scalar values.
- Collections, `Array<T>` and maps are validated per item (map values for maps). Primitive arrays such as `IntArray`
  are scalar values.
- Typealiases are expanded, so `typealias Email = String` behaves like `String`.
- JSON validators read scalar values as `String`, `Int`, `Long`, `Float`, `Double`, `Boolean`, `BigDecimal`, `UUID`,
  `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime` and `Instant`, plus enums.
- JSON validators report enum values that match no entry as `EnumValues`. Typed validators accept any entry, unless you
  implement a validation function for the enum such as `fun validateKind(kind: Kind?, call: ValidationCall)`.

## Polymorphic types

A value is validated as its runtime subtype when its declared type is `sealed` or lists its subtypes with
`@JsonSubTypes`.

Typed validators dispatch on the runtime class, most specific subtypes first:

```kotlin
when (animal) {
    is Cat -> validateCat(animal, call)
    is Dog -> validateDog(animal, call)
}
```

When the dispatch does not cover every possible subtype (an open or abstract type with `@JsonSubTypes`, or a sealed
type with subtypes the processor cannot validate), an `else` branch validates the properties declared on the base type.

JSON validators dispatch on the type id when the base type or one of its supertypes has `@JsonTypeInfo`:

```kotlin
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
sealed interface Animal

@JsonTypeName("CAT")
data class Cat(@field:NotBlank val favoritePrey: String) : Animal
```

- The candidate subtypes are the concrete classes reachable through `@JsonSubTypes` and sealed subclasses, at any depth.
- Type ids come from `@JsonSubTypes.Type(name = ..., names = [...])`, then `@JsonTypeName`, then Jackson's default:
  the class name without its package (`Outer$Inner`) for `Id.NAME`, or the simple name for `Id.SIMPLE_NAME`.
- The type id property (`@type` unless `property` is set) is reported as `NotNull` when missing, `Type` when it is not a
  string, and `EnumValues` when it matches no subtype. The subtype's properties are only checked when the id is known.
- Supported settings are `use = Id.NAME` or `Id.SIMPLE_NAME` with `include = As.PROPERTY` or `As.EXISTING_PROPERTY`.
  `use = Id.NONE` turns dispatch off.

Overriding properties inherit the constraints of the properties they override, so `@get:NotBlank val name: String` on
`Animal` also applies to `Cat.name`. A constraint of the same annotation type on the override replaces the inherited one.

## Build diagnostics

The processor reports problems as compiler errors that point at the offending interface, function or property, and
skips generating that validator:

- abstract functions with an unsupported shape, or a JSON function without `@ValidationParamType`;
- properties with types the processor cannot resolve (type parameters, star projections, unresolved types), and
  JSON-mode scalars without a JSON reader (for example `URI` or `ZonedDateTime`);
- built-in constraints on types they do not support, for example `@NotBlank` on an `Int` or `@Past` on a `Duration`;
- subtypes that are generic or do not extend the type listing them in `@JsonSubTypes`;
- in JSON mode, unsupported `@JsonTypeInfo` settings and a type id that maps to more than one subtype.

Constraint annotations without a descriptor (anything in `jakarta.validation.constraints` or meta-annotated with
`@Constraint`) are ignored with a warning. Set the KSP option `ktor.validation.strict` to `true` to fail the build
instead:

```kotlin
ksp {
    arg("ktor.validation.strict", "true")
}
```

## Which properties are validated

- Public properties that have a backing field, including properties inherited from superclasses.
  Computed properties (`val isAdult get() = age >= 18`) are skipped.
- Abstract properties declared by interfaces or abstract classes.
- Constraint annotations are collected from the property, its getter, its field and the matching primary-constructor
  parameter, so `@field:`, `@get:`, `@param:` and bare annotations all apply.
- Repeated constraints (`@field:Pattern(...) @field:Pattern(...)`) and their containers (`@Pattern.List(...)`) all
  apply, in declaration order.
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

## Temporal constraints

`@Past`, `@PastOrPresent`, `@Future` and `@FutureOrPresent` support `Date`, `Calendar`, `Instant`, `LocalDate`,
`LocalDateTime`, `LocalTime`, `MonthDay`, `OffsetDateTime`, `OffsetTime`, `Year`, `YearMonth`, `ZonedDateTime` and the
`java.time.chrono` dates. Instants, offset and zoned date-times, `Date` and `Calendar` are compared as points in time;
local types are compared with the current date or time in the clock's zone.

Generated validators read the current time from a `java.time.Clock`, `Clock.systemDefaultZone()` by default. Pass
another clock to the constructor to fix the time in tests or to validate in a different zone:

```kotlin
val validator = UserValidatorImpl(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC))
```

Custom constraints can read the same clock from `call.clock`.

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
defaults:
  className: com.example.EvenConstraints
constraints:
  - annotation: com.example.Even
  - annotation: com.example.Present
    rejectsNull: true
    supportedTypes:
      - kotlin.String
      - kotlin.collections.Collection
  - annotation: com.example.Iban
    className: com.example.BankConstraints
    json:
      className: com.example.JsonBankConstraints
```

By default a constraint is called as `EvenConstraints.checkEven(prop, value, call, <annotation args>)`, and only when
the value is present. Annotation arguments are passed as named arguments, rendered as Kotlin literals: strings, chars,
numbers, enum entries, `KClass` references, arrays (as `arrayOf(...)`) and nested annotations are supported.
Set `rejectsNull: true` to make the property required; a missing value is then reported as `NotNull`.
Set `className` to call a constraint on another class or object than `defaults.className`; class names must be fully
qualified. Set `methodName` to call another method than `check` + the annotation's simple name, for example when a
custom `com.example.Email` sits next to `jakarta.validation.constraints.Email`. Constraints are matched by their fully
qualified annotation name, so both apply.

JSON validators call the same method unless `json.className` or `json.methodName` points them elsewhere, for example
to an overload that accepts the `ArrayNode` or `ObjectNode` of a list or map property.

`supportedTypes` lists the fully qualified Kotlin types the constraint may annotate; the build fails when it is
applied to any other type. `kotlin.collections.Collection` matches any collection or `Array`, and
`kotlin.collections.Map` matches any map. Without `supportedTypes`, the type is not checked.
