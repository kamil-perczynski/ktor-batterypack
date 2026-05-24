package io.github.kperczynski.controllers

import io.github.kperczynski.domain.user.UserCreate
import io.github.kperczynski.domain.user.UserUpdate
import io.github.kperczynski.libs.exception.FieldError
import io.github.kperczynski.libs.exception.ValidationException
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import io.konform.validation.constraints.minLength
import org.koin.core.annotation.Singleton

@Singleton
class UserDtoValidator {

    private val createValidator = Validation {
        UserCreate::name {
            minLength(1) hint "Name must not be empty" userContext mapOf(
                "code" to "minLength",
                "min" to 2
            )
            minLength(2) hint "Name must be at least 2 characters" userContext mapOf(
                "code" to "minLength",
                "min" to 2
            )
        }
        UserCreate::age {
            constrain("must be at least 0") { it >= 0 } userContext mapOf(
                "code" to "minValue",
                "min" to 0
            )
            constrain("must be at most 150") { it <= 150 } userContext mapOf(
                "code" to "maxValue",
                "max" to 150
            )
        }
    }

    private val updateValidator = Validation {
        UserUpdate::name {
            minLength(1) hint "Name must not be empty" userContext mapOf(
                "code" to "minLength",
                "min" to 2
            )
            minLength(2) hint "Name must be at least 2 characters" userContext mapOf(
                "code" to "minLength",
                "min" to 2
            )
        }
        UserUpdate::age {
            constrain("must be at least 0") { it >= 0 } userContext mapOf(
                "code" to "minValue",
                "min" to 0
            )
            constrain("must be at most 150") { it <= 150 } userContext mapOf(
                "code" to "maxValue",
                "max" to 150
            )
        }
    }

    fun validateCreate(value: UserCreate) {
        val result = createValidator(value)
        checkValid(result)
    }

    fun validateUpdate(value: UserUpdate) {
        val result = updateValidator(value)
        checkValid(result)
    }
}

private fun checkValid(result: ValidationResult<*>) {
    if (!result.isValid) {
        val errors = result.errors.map {
            FieldError(it.dataPath, it.message, it.userContext as Map<String, Any>)
        }

        throw ValidationException(errors)
    }
}
