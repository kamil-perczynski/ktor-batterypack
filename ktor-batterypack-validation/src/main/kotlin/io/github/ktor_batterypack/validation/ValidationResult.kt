package io.github.ktor_batterypack.validation

sealed interface ValidationResult<U> {

    companion object {
        fun <T> valid(data: T): ValidationResult<T> {
            return ValidResult(data)
        }

        fun <T : Any> invalid(data: T, errors: ConstraintError): ValidationResult<T> {
            return InvalidResult(data, errors)
        }
    }

    val isValid: Boolean
    val isInvalid: Boolean

    fun <T> fold(
        onValid: (validResult: U) -> T,
        onInvalid: (data: U, errors: ConstraintError) -> T,
    ): T

}

data class ValidResult<U>(val data: U) : ValidationResult<U> {
    override val isValid: Boolean = true
    override val isInvalid: Boolean = false

    override fun <T> fold(
        onValid: (validResult: U) -> T,
        onInvalid: (data: U, errors: ConstraintError) -> T
    ): T {
        return onValid(data)
    }
}

data class InvalidResult<U>(val data: U, val errors: ConstraintError) : ValidationResult<U> {
    override val isValid: Boolean = false
    override val isInvalid: Boolean = true

    override fun <T> fold(
        onValid: (validResult: U) -> T,
        onInvalid: (data: U, errors: ConstraintError) -> T
    ): T {
        return onInvalid(data, errors)
    }
}

fun <T> ValidationResult<T>.check(): T {
    return fold(
        onValid = { it },
        onInvalid = { data, errors ->
            throw ValidationException(data as Any, errors)
        }
    )
}