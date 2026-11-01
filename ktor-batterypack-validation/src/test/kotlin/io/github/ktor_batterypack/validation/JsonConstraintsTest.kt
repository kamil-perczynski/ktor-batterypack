package io.github.ktor_batterypack.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

class JsonConstraintsTest {

    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `NotEmpty rejects an empty object`() {
        val call = ValidationCall()

        JsonConstraints.checkNotEmpty("filled", readObject("""{"a": 1}"""), call)
        JsonConstraints.checkNotEmpty("empty", readObject("{}"), call)

        val errors = call.finishObject()?.properties
        assertThat(errors?.keys).containsExactly("empty")
        assertThat((errors?.get("empty") as FieldConstraintError).errors)
            .containsExactly(SingleConstraintError("NotEmpty", "Must not be empty object"))
    }

    @Test
    fun `Size counts object entries`() {
        val call = ValidationCall()

        JsonConstraints.checkSize("small", readObject("""{"a": 1}"""), call, min = 0, max = 1)
        JsonConstraints.checkSize("large", readObject("""{"a": 1, "b": 2}"""), call, min = 0, max = 1)

        assertThat(errorKeysOf(call)).containsExactly("large")
    }

    @Test
    fun `Size reports the allowed object size`() {
        val call = ValidationCall()

        JsonConstraints.checkSize("large", readObject("""{"a": 1, "b": 2}"""), call, min = 0, max = 1)

        assertThat(errorsOf(call, "large")).containsExactly(
            SingleConstraintError("Size", "Must have size between [0, 1]")
        )
    }

    private fun readObject(json: String): ObjectNode {
        return jsonMapper.readTree(json) as ObjectNode
    }

}

private fun errorKeysOf(call: ValidationCall): Set<String> {
    return call.finishObject()?.properties?.keys.orEmpty()
}

private fun errorsOf(call: ValidationCall, prop: String): List<SingleConstraintError> {
    val error = call.finishObject()?.properties?.get(prop) as FieldConstraintError?
    return error?.errors.orEmpty()
}
