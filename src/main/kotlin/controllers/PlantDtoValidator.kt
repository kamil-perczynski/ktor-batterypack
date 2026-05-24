package io.github.kperczynski.controllers

import io.github.kperczynski.libs.exception.FieldError
import io.github.kperczynski.libs.exception.ValidationException
import io.konform.validation.Validation
import io.konform.validation.jsonschema.minLength
import org.koin.core.annotation.Singleton

@Singleton
class PlantDtoValidator {

    fun validateIdentification(tempIdentityId: String) {
        if (tempIdentityId.isBlank()) {
            throw ValidationException(listOf(FieldError("tempIdentityId", "tempIdentityId must not be empty")))
        }
    }
}
