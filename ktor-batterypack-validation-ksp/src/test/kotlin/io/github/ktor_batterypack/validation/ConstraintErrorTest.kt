package io.github.ktor_batterypack.validation

import io.github.ktor_batterypack.validation.model.Person
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class ConstraintErrorTest {

    private val jsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    @Test
    fun `serialize ObjectConstraintViolations for Person model`() {
        val personViolations = someConstraintErrors()

        val json = jsonMapper.writeValueAsString(personViolations)

        println(json)
    }

    @Test
    fun name() {
        val result = ValidationResult.invalid<Person>(someConstraintErrors())

        val foldResult = result.fold(
            onValid = {
                1
            },
            onInvalid = {
                println("Validation failed: $it")
                0
            },
        )

        println(foldResult)
    }
}

private fun someConstraintErrors(): ObjectConstraintError = ObjectConstraintError(
    properties = mapOf(
        "firstName" to FieldConstraintError(
            errors = listOf(
                SingleConstraintError(constraint = "NotBlank")
            )
        ),
        "lastName" to FieldConstraintError(
            errors = listOf(
                SingleConstraintError(constraint = "NotBlank")
            )
        ),
        "address" to ObjectConstraintError(
            properties = mapOf(
                "zipCode" to FieldConstraintError(
                    errors = listOf(
                        SingleConstraintError(constraint = "NotBlank"),
                        SingleConstraintError(constraint = "Pattern")
                    )
                )
            )
        ),
        "birthDate" to FieldConstraintError(
            errors = listOf(
                SingleConstraintError(constraint = "Past")
            )
        ),
        "identifications" to ListConstraintError(
            errors = listOf(
                SingleConstraintError(constraint = "NotEmpty")
            ),
            items = listOf(
                ObjectConstraintError(
                    properties = mapOf(
                        "confidence" to FieldConstraintError(
                            errors = listOf(
                                SingleConstraintError(constraint = "Min"),
                                SingleConstraintError("Max")
                            )
                        )
                    )
                ),
                null,
                null
            )
        )
    )
)


