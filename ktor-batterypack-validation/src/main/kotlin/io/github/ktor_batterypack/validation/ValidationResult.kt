package io.github.ktor_batterypack.validation

sealed interface ValidationResult<U> {

    companion object {
        fun <T> valid(data: T): ValidationResult<T> {
            return ValidResult(data)
        }

        fun <T : Any> invalid(errors: ConstraintError): ValidationResult<T> {
            return InvalidResult(errors)
        }
    }

    val isValid: Boolean
    val isInvalid: Boolean

    fun <T> fold(
        onValid: (validationResult: U) -> T,
        onInvalid: (validationResult: ConstraintError) -> T,
    ): T
}

data class ValidResult<U>(val data: U) : ValidationResult<U> {
    override val isValid: Boolean = true
    override val isInvalid: Boolean = false

    override fun <T> fold(
        onValid: (validationResult: U) -> T,
        onInvalid: (validationResult: ConstraintError) -> T
    ): T {
        return onValid(data)
    }
}

data class InvalidResult<U>(val errors: ConstraintError) : ValidationResult<U> {
    override val isValid: Boolean = false
    override val isInvalid: Boolean = true

    override fun <T> fold(
        onValid: (validationResult: U) -> T,
        onInvalid: (validationResult: ConstraintError) -> T
    ): T {
        return onInvalid(errors)
    }
}