package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.model.Address
import io.github.ktor_batterypack.validation.model.Person
import io.github.ktor_batterypack.validation.model.PersonIdentification
import io.github.ktor_batterypack.validation.model.PersonIdentificationType.*
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.LocalDate

class PersonValidatorTest {

    private val jsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    @Test
    fun testInvalidObject() {
        val validator = GeneratedPersonValidatorImpl()

        val person = Person(
            firstName = null,
            lastName = null,
            address = Address(
                addressLine1 = "123 Main Street",
                addressLine2 = "",
                zipCode = "",
            ),
            birthDate = LocalDate.parse("1995-01-12"),
            identifications = listOf(
                PersonIdentification(ID_DOCUMENT_CHECK, 50.0),
                PersonIdentification(LIVENESS_CHECK, 50.0),
                PersonIdentification(SIGNATURE_SPECIMEN_1, 150.0)
            ),
        )

        val result = validator.validate(person)

        val json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)

        assertThat(json).isEqualTo(
            """
                {
                  "errors" : {
                    "identifications" : {
                      "errors" : [ ],
                      "items" : [ null, null, {
                        "confidence" : {
                          "errors" : [ {
                            "constraint" : "Max",
                            "message" : "Must be at most 100"
                          } ]
                        }
                      } ]
                    },
                    "address" : {
                      "zipCode" : {
                        "errors" : [ {
                          "constraint" : "NotBlank"
                        }, {
                          "constraint" : "Pattern",
                          "message" : "Must match \\d{2}-\\d{3}"
                        } ]
                      }
                    }
                  },
                  "isInvalid" : true,
                  "isValid" : false
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
                PersonIdentification(ID_DOCUMENT_CHECK, 50.0)
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
                      "type" : "ID_DOCUMENT_CHECK",
                      "confidence" : 50.0
                    } ]
                  },
                  "isInvalid" : false,
                  "isValid" : true
                }
            """.trimIndent()
        )
    }
}