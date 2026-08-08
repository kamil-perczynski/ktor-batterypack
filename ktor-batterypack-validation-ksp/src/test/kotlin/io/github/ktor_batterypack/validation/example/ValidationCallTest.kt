package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.SingleConstraintError
import io.github.ktor_batterypack.validation.ValidationCall
import org.junit.jupiter.api.Test

class ValidationCallTest {

    @Test
    fun name() {
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

        println(objectConstraintError)
    }
}