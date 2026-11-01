package io.github.ktor_batterypack.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime

class JsonTypeChecksTest {

    private val jsonMapper = JsonMapper.builder().build()

    @Test
    fun `explicit null is treated as absent by every type check`() {
        val node = readTree("""{"value": null}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkInt("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLong("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkFloat("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkDouble("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkBigDecimal("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkBoolean("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkString("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkArray("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkObject("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkUuid("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalDate("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalDateTime("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalTime("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkOffsetDateTime("value", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInstant("value", node, call)).isNull()
        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `explicit null fails NotNull only once`() {
        val node = readTree("""{"value": null}""")
        val call = ValidationCall()

        JsonTypeChecks.checkNotNull("value", node, call)
        JsonTypeChecks.checkInt("value", node, call)

        assertThat(errorsOf(call, "value")).containsExactly(SingleConstraintError("NotNull", "Must not be null"))
    }

    @Test
    fun `checkInt accepts integral numbers and integral strings`() {
        val node = readTree("""{"number": 42, "string": "-7"}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkInt("number", node, call)).isEqualTo(42)
        assertThat(JsonTypeChecks.checkInt("string", node, call)).isEqualTo(-7)
        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `checkInt reports fractional and out of range values instead of throwing`() {
        val node = readTree(
            """{"fraction": 3.7, "exponent": 1e20, "fractionString": "1.5", "overflowString": "99999999999", "overflow": 99999999999}"""
        )
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkInt("fraction", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInt("exponent", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInt("fractionString", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInt("overflowString", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInt("overflow", node, call)).isNull()

        assertThat(errorsOf(call, "fraction")).containsExactly(SingleConstraintError("Type", "Expected integer"))
        assertThat(errorsOf(call, "exponent")).containsExactly(SingleConstraintError("Type", "Expected integer"))
        assertThat(errorsOf(call, "fractionString")).containsExactly(SingleConstraintError("Type", "Expected integer"))
        assertThat(errorsOf(call, "overflowString")).containsExactly(
            SingleConstraintError("Range", "Value is out of range for integer")
        )
        assertThat(errorsOf(call, "overflow")).containsExactly(
            SingleConstraintError("Range", "Value is out of range for integer")
        )
    }

    @Test
    fun `checkLong reports out of range values instead of throwing`() {
        val node = readTree("""{"overflow": 99999999999999999999, "overflowString": "99999999999999999999"}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkLong("overflow", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLong("overflowString", node, call)).isNull()

        assertThat(errorsOf(call, "overflow")).containsExactly(
            SingleConstraintError("Range", "Value is out of range for long")
        )
        assertThat(errorsOf(call, "overflowString")).containsExactly(
            SingleConstraintError("Range", "Value is out of range for long")
        )
    }

    @Test
    fun `numeric checks report non numeric strings and wrong node types`() {
        val node = readTree("""{"text": "abc", "flag": true}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkDouble("text", node, call)).isNull()
        assertThat(JsonTypeChecks.checkBigDecimal("flag", node, call)).isNull()

        assertThat(errorsOf(call, "text")).containsExactly(SingleConstraintError("Type", "Expected number"))
        assertThat(errorsOf(call, "flag")).containsExactly(
            SingleConstraintError("Type", "Expected number but was boolean")
        )
    }

    @Test
    fun `floating point checks convert numbers and numeric strings`() {
        val node = readTree("""{"float": 1.5, "double": "2.25", "decimal": 10.10, "decimalString": "0.30"}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkFloat("float", node, call)).isEqualTo(1.5f)
        assertThat(JsonTypeChecks.checkDouble("double", node, call)).isEqualTo(2.25)
        assertThat(JsonTypeChecks.checkBigDecimal("decimal", node, call)).isEqualByComparingTo(BigDecimal("10.10"))
        assertThat(JsonTypeChecks.checkBigDecimal("decimalString", node, call)).isEqualTo(BigDecimal("0.30"))
        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `checkFloat reports values outside of float range`() {
        val node = readTree("""{"value": 1e300}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkFloat("value", node, call)).isNull()

        assertThat(errorsOf(call, "value")).containsExactly(
            SingleConstraintError("Range", "Value is out of range for float")
        )
    }

    @Test
    fun `date and time checks parse valid values`() {
        val node = readTree(
            """
            {
              "date": "2024-02-29",
              "dateTime": "2024-02-29T10:15:30.123",
              "dateTimeWithoutSeconds": "2024-02-29T10:15",
              "time": "23:59:59",
              "timeWithoutSeconds": "10:15",
              "offsetDateTime": "2024-02-29T10:15:30+01:00",
              "instant": "2024-02-29T10:15:30Z"
            }
            """
        )
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkLocalDate("date", node, call)).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(JsonTypeChecks.checkLocalDateTime("dateTime", node, call))
            .isEqualTo(LocalDateTime.parse("2024-02-29T10:15:30.123"))
        assertThat(JsonTypeChecks.checkLocalDateTime("dateTimeWithoutSeconds", node, call))
            .isEqualTo(LocalDateTime.of(2024, 2, 29, 10, 15))
        assertThat(JsonTypeChecks.checkLocalTime("time", node, call)).isEqualTo(LocalTime.of(23, 59, 59))
        assertThat(JsonTypeChecks.checkLocalTime("timeWithoutSeconds", node, call)).isEqualTo(LocalTime.of(10, 15))
        assertThat(JsonTypeChecks.checkOffsetDateTime("offsetDateTime", node, call))
            .isEqualTo(OffsetDateTime.parse("2024-02-29T10:15:30+01:00"))
        assertThat(JsonTypeChecks.checkInstant("instant", node, call)).isEqualTo(Instant.parse("2024-02-29T10:15:30Z"))
        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `malformed dates report Format errors instead of throwing`() {
        val node = readTree(
            """
            {
              "date": "2024-13-45",
              "dateTime": "2023-02-29T10:15:30",
              "time": "25:61:00",
              "offsetDateTime": "2024-02-30T25:61:00Z",
              "instant": "2024-02-29T25:00:00Z",
              "shape": "29.02.2024"
            }
            """
        )
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkLocalDate("date", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalDateTime("dateTime", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalTime("time", node, call)).isNull()
        assertThat(JsonTypeChecks.checkOffsetDateTime("offsetDateTime", node, call)).isNull()
        assertThat(JsonTypeChecks.checkInstant("instant", node, call)).isNull()
        assertThat(JsonTypeChecks.checkLocalDate("shape", node, call)).isNull()

        assertThat(errorsOf(call, "date")).containsExactly(SingleConstraintError("Format", "Must be a valid local date"))
        assertThat(errorsOf(call, "dateTime")).containsExactly(
            SingleConstraintError("Format", "Must be a valid local date-time")
        )
        assertThat(errorsOf(call, "time")).containsExactly(SingleConstraintError("Format", "Must be a valid local time"))
        assertThat(errorsOf(call, "offsetDateTime")).containsExactly(
            SingleConstraintError("Format", "Must be a valid offset date-time")
        )
        assertThat(errorsOf(call, "instant")).containsExactly(SingleConstraintError("Format", "Must be a valid instant"))
        assertThat(errorsOf(call, "shape")).containsExactly(SingleConstraintError("Format", "Must be a valid local date"))
    }

    @Test
    fun `checkOffsetDateTime validates list items`() {
        val items = readTree("""["2024-02-29T10:15:30Z", "nope"]""")
        val first = ValidationCall()
        val second = ValidationCall()

        assertThat(JsonTypeChecks.checkOffsetDateTime("$", items.get(0), first))
            .isEqualTo(OffsetDateTime.parse("2024-02-29T10:15:30Z"))
        assertThat(JsonTypeChecks.checkOffsetDateTime("$", items.get(1), second)).isNull()

        assertThat(first.finishObject()).isNull()
        assertThat(errorsOf(second, "$")).containsExactly(
            SingleConstraintError("Format", "Must be a valid offset date-time")
        )
    }

    @Test
    fun `checkTypeId returns a known type id`() {
        val node = readTree("""{"kind": "CAT"}""")
        val call = ValidationCall()

        assertThat(JsonTypeChecks.checkTypeId("kind", node, call, setOf("CAT", "DOG"))).isEqualTo("CAT")
        assertThat(call.finishObject()).isNull()
    }

    @Test
    fun `checkTypeId reports missing, mistyped and unknown type ids`() {
        val missing = ValidationCall()
        val mistyped = ValidationCall()
        val unknown = ValidationCall()

        assertThat(JsonTypeChecks.checkTypeId("kind", readTree("""{}"""), missing, setOf("CAT"))).isNull()
        assertThat(JsonTypeChecks.checkTypeId("kind", readTree("""{"kind": 1}"""), mistyped, setOf("CAT"))).isNull()
        assertThat(JsonTypeChecks.checkTypeId("kind", readTree("""{"kind": "COW"}"""), unknown, setOf("CAT"))).isNull()

        assertThat(errorsOf(missing, "kind")).containsExactly(SingleConstraintError("NotNull", "Must not be null"))
        assertThat(errorsOf(mistyped, "kind")).containsExactly(SingleConstraintError("Type", "Expected string but was number"))
        assertThat(errorsOf(unknown, "kind")).containsExactly(SingleConstraintError("EnumValues", "Allowed values are: [CAT]"))
    }

    private fun readTree(json: String): JsonNode {
        return jsonMapper.readTree(json)
    }

}

private fun errorsOf(call: ValidationCall, prop: String): List<SingleConstraintError> {
    val error = call.finishObject()?.properties?.get(prop) as FieldConstraintError?
    return error?.errors.orEmpty()
}
