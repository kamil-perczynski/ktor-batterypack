package io.github.ktor_batterypack.validation

object StringFormats {

    private val UUID_PATTERN =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val LOCAL_DATE_PATTERN =
        Regex("^\\d{4}-\\d{2}-\\d{2}$")

    private val LOCAL_DATE_TIME_PATTERN =
        Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$")

    private const val OFFSET_PATTERN = "(Z|[+-]\\d{2}:\\d{2})"

    private val OFFSET_DATE_TIME_PATTERN =
        Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$OFFSET_PATTERN")

    private val INSTANT_PATTERN =
        Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$OFFSET_PATTERN$")

    fun checkUuid(prop: String, value: String, call: ValidationCall): Boolean {
        if (!UUID_PATTERN.matches(value)) {
            call.propertyError(prop, SingleConstraintError("Format", "Must be a valid UUID"))
            return false
        }

        return true
    }

    fun checkLocalDate(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!LOCAL_DATE_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid local date"))
            return false
        }
        return true
    }

    fun checkLocalDateTime(prop: String, value: String, call: ValidationCall) {
        if (!LOCAL_DATE_TIME_PATTERN.matches(value)) {
            call.propertyError(prop, SingleConstraintError("Format", "Must be a valid local date-time"))
        }
    }

    fun checkOffsetDateTime(prop: String, value: String, call: ValidationCall): Boolean {
        if (!OFFSET_DATE_TIME_PATTERN.matches(value)) {
            call.propertyError(prop, SingleConstraintError("Format", "Must be a valid offset date-time"))
            return false
        }
        return true
    }

    fun checkInstant(prop: String, value: String, call: ValidationCall) {
        if (!INSTANT_PATTERN.matches(value)) {
            call.propertyError(prop, SingleConstraintError("Format", "Must be a valid instant"))
        }
    }

    fun checkEnum(prop: String, value: String, call: ValidationCall, values: Collection<String>) {
        if (value !in values) {
            call.propertyError(prop, SingleConstraintError("Format", "Must be one of $values"))
        }
    }

}
