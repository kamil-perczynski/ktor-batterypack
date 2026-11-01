package io.github.ktor_batterypack.validation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.jupiter.api.Test

class ValidationCallTest {

    @Test
    fun `object errors keep the order in which properties were first reported`() {
        val call = ValidationCall()

        call.propertyError("zeta", SingleConstraintError("NotNull"))
        call.nestedProperty("alpha").apply {
            propertyError("street", SingleConstraintError("NotBlank"))
            finishObject()
        }
        call.propertyError("mu", SingleConstraintError("NotNull"))
        call.propertyError("zeta", SingleConstraintError("NotBlank"))
        call.directError(SingleConstraintError("Custom"))

        val error = call.finishObject()

        assertThat(error?.properties?.keys).containsExactly("zeta", "alpha", "mu", "$")
        assertThat(error?.properties?.get("zeta")).isEqualTo(
            FieldConstraintError(listOf(SingleConstraintError("NotNull"), SingleConstraintError("NotBlank")))
        )
    }

    @Test
    fun `property errors override nested errors registered under the same key`() {
        val call = ValidationCall()

        call.nestedProperty("address").apply {
            propertyError("street", SingleConstraintError("NotBlank"))
            finishObject()
        }
        call.propertyError("address", SingleConstraintError("Custom"))

        val error = call.finishObject()

        assertThat(error?.properties).containsExactly(
            entry("address", FieldConstraintError(listOf(SingleConstraintError("Custom"))))
        )
    }

}
