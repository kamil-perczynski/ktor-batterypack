package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.ksp.CodegenModelResolver
import io.github.ktor_batterypack.validation.ksp.ValidatorCodegen
import io.github.ktor_batterypack.validation.model.Address
import io.github.ktor_batterypack.validation.model.LegacyIdentification
import io.github.ktor_batterypack.validation.model.Person
import io.github.ktor_batterypack.validation.model.PersonIdentificationType.*
import io.github.ktor_batterypack.validation.reflection.ReflectionValidatorInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

class PersonValidatorTest {

    private val jsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    @Test
    @Disabled
    fun testGenerateAndWritePersonValidator() {
        val sourceRoot = Paths.get("src/test/kotlin").toAbsolutePath()

        val codegenModelResolver = CodegenModelResolver()

        val model = codegenModelResolver.resolve(
            ReflectionValidatorInterface(PersonValidator::class)
        )

        val validatorClass = ValidatorCodegen().generateValidatorClass(model)

        val filePath = Paths.get(
            sourceRoot.toString(),
            "io/github/ktor_batterypack/validation/example/PersonValidatorImpl.kt"
        )

        Files.writeString(filePath, validatorClass)
    }

    @Test
    fun testInvalidObject() {
        val validator = GeneratedPersonValidatorImpl()

        val person = Person(
            firstName = "",
            lastName = "",
            address = Address(
                addressLine1 = "123 Main Street",
                addressLine2 = "",
                zipCode = "",
            ),
            birthDate = LocalDate.parse("1995-01-12"),
            identifications = listOf(
                LegacyIdentification(ID_DOCUMENT_CHECK),
                LegacyIdentification(LIVENESS_CHECK),
                LegacyIdentification(SIGNATURE_SPECIMEN_1)
            ),
        )

        val result = validator.validate(person)

        val json = jsonMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(result.fold({ it }, { _, errors -> errors }))

        assertThat(json).isEqualTo(
            """
                {
                  "firstName" : [ {
                    "constraint" : "NotBlank"
                  } ],
                  "lastName" : [ {
                    "constraint" : "NotBlank"
                  } ],
                  "address" : {
                    "zipCode" : [ {
                      "constraint" : "NotBlank"
                    }, {
                      "constraint" : "Pattern",
                      "message" : "Must match \\d{2}-\\d{3}"
                    } ]
                  }
                }
            """.trimIndent()
        )
    }

    @Test
    fun testValidObject() {
        val validator = GeneratedPersonValidatorImpl()

        val person = Person(
            firstName = "John",
            lastName = "Doe",
            address = Address(
                addressLine1 = "123 Main Street",
                addressLine2 = "NYC",
                zipCode = "12-232",
            ),
            birthDate = LocalDate.parse("1995-01-12"),
            identifications = listOf(
                LegacyIdentification(ID_DOCUMENT_CHECK)
            ),
        )

        val result = validator.validate(person)

        val json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)

        assertThat(json).isEqualTo(
            """
                {
                  "data" : {
                    "firstName" : "John",
                    "lastName" : "Doe",
                    "address" : {
                      "addressLine1" : "123 Main Street",
                      "addressLine2" : "NYC",
                      "zipCode" : "12-232"
                    },
                    "birthDate" : "1995-01-12",
                    "identifications" : [ {
                      "type" : "ID_DOCUMENT_CHECK"
                    } ],
                    "creditCards" : [ ]
                  },
                  "isInvalid" : false,
                  "isValid" : true
                }
            """.trimIndent()
        )
    }
}