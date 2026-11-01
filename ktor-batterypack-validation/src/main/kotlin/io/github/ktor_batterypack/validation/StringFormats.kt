package io.github.ktor_batterypack.validation

object StringFormats {

    private val UUID_PATTERN =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private const val DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}"

    private const val TIME_PATTERN = "\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?"

    private const val OFFSET_PATTERN = "(Z|[+-]\\d{2}:\\d{2})"

    private val LOCAL_DATE_PATTERN =
        Regex("^$DATE_PATTERN$")

    private val LOCAL_TIME_PATTERN =
        Regex("^$TIME_PATTERN$")

    private val LOCAL_DATE_TIME_PATTERN =
        Regex("^${DATE_PATTERN}T$TIME_PATTERN$")

    private val OFFSET_DATE_TIME_PATTERN =
        Regex("^${DATE_PATTERN}T$TIME_PATTERN$OFFSET_PATTERN$")

    private val INSTANT_PATTERN =
        Regex("^${DATE_PATTERN}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?$OFFSET_PATTERN$")

    @JvmStatic
    fun checkUuid(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!UUID_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid UUID"))
            return false
        }

        return true
    }

    @JvmStatic
    fun checkLocalDate(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!LOCAL_DATE_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid local date"))
            return false
        }
        return true
    }

    @JvmStatic
    fun checkLocalTime(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!LOCAL_TIME_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid local time"))
            return false
        }
        return true
    }

    @JvmStatic
    fun checkLocalDateTime(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!LOCAL_DATE_TIME_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid local date-time"))
            return false
        }
        return true
    }

    @JvmStatic
    fun checkOffsetDateTime(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!OFFSET_DATE_TIME_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid offset date-time"))
            return false
        }
        return true
    }

    @JvmStatic
    fun checkInstant(prop: String, value: String, call: ValidationCall? = null): Boolean {
        if (!INSTANT_PATTERN.matches(value)) {
            call?.propertyError(prop, SingleConstraintError("Format", "Must be a valid instant"))
            return false
        }
        return true
    }

}
