package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.SingleConstraintError
import io.github.ktor_batterypack.validation.ValidationCall
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class ValidationCallTest {

    @Test
    fun testValidationErrorBuilder() {
        val call = ValidationCall()

        call.apply {
            propertyError("firstName", SingleConstraintError("NotNull"))
            propertyError("firstName", SingleConstraintError("NotBlank"))
            propertyError("lastName", SingleConstraintError("NotNull"))
            propertyError("createdAt", SingleConstraintError("NotNull"))
            propertyError("lastModified", SingleConstraintError("NotBlank"))
        }

        val address = call.nestedProperty("address").apply {
            propertyError("addressLine2", SingleConstraintError("Empty Address"))
            finishObject()
        }

        val identifications = call.nestedProperty("identifications").apply {
            directError(SingleConstraintError("NotNull"))
            directError(SingleConstraintError("NotEmpty"))

            listItem().apply {
                finishObject()
            }
            listItem().apply {
                propertyError("firstName", SingleConstraintError("NotNull"))
                propertyError("firstName", SingleConstraintError("NotBlank"))
                propertyError("lastName", SingleConstraintError("NotNull"))
                finishObject()
            }
            listItem().apply {
                finishObject()
            }
            finishList()
        }

        val objectConstraintError = call.finishObject()

        val jsonMapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .build()

        val json = jsonMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(objectConstraintError)

        assertThat(json).isEqualTo(
            """
                {
                  "firstName" : {
                    "errors" : [ {
                      "constraint" : "NotNull"
                    }, {
                      "constraint" : "NotBlank"
                    } ]
                  },
                  "lastName" : {
                    "errors" : [ {
                      "constraint" : "NotNull"
                    } ]
                  },
                  "createdAt" : {
                    "errors" : [ {
                      "constraint" : "NotNull"
                    } ]
                  },
                  "address" : {
                    "addressLine2" : {
                      "errors" : [ {
                        "constraint" : "Empty Address"
                      } ]
                    }
                  },
                  "lastModified" : {
                    "errors" : [ {
                      "constraint" : "NotBlank"
                    } ]
                  },
                  "identifications" : {
                    "errors" : [ {
                      "constraint" : "NotNull"
                    }, {
                      "constraint" : "NotEmpty"
                    } ],
                    "items" : [ null, {
                      "firstName" : {
                        "errors" : [ {
                          "constraint" : "NotNull"
                        }, {
                          "constraint" : "NotBlank"
                        } ]
                      },
                      "lastName" : {
                        "errors" : [ {
                          "constraint" : "NotNull"
                        } ]
                      }
                    }, null ]
                  }
                }
            """.trimIndent()
        )
    }
}