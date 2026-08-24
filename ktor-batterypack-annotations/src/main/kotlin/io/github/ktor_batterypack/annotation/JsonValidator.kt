package io.github.ktor_batterypack.annotation

/**
 * Marks an interface as a validator contract. The KSP processor generates
 * an implementation class that validates each method's single parameter
 * (typically a DTO) by checking all constructor properties.
 *
 * Each method in the interface must accept exactly one parameter of a
 * data class type. The generated implementation applies the following
 * rules to every property of that DTO (and any nested DTOs):
 * - Non-nullable [String] properties must not be blank
 * - Non-nullable numeric properties must be positive
 * - All other properties are left unvalidated
 *
 * Usage:
 * ```kotlin
 * @Validator
 * interface CreateUserValidator {
 *     fun validate(dto: CreateUserDto)
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
annotation class JsonValidator
