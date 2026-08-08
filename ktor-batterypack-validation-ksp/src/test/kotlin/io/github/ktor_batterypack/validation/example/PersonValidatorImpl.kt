package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.SingleConstraintError
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.model.Address
import io.github.ktor_batterypack.validation.model.Person
import io.github.ktor_batterypack.validation.model.PersonIdentification
import io.github.ktor_batterypack.validation.ValidationResult
import kotlin.text.isBlank

@Suppress("SENSELESS_COMPARISON")
class PersonValidatorImpl : PersonValidator {

    override fun validate(person: Person): ValidationResult<Person> {
        val call = ValidationCall()

        checkNotNull("firstName", person.firstName, call)
        if (person.firstName != null) {
            checkNotBlank("firstName", person.firstName, call)
        }

        checkNotNull("lastName", person.firstName, call)
        if (person.firstName != null) {
            checkNotBlank("lastName", person.firstName, call)
        }

        checkNotNull("address", person.address, call)
        if (person.address != null) {
            validateAddress(person.address, call.nestedProperty("address"))
            call.finishObject()
        }

        checkNotNull("identifications", person.identifications, call)
        if (person.identifications != null) {
            validateIdentifications(person.identifications, call.nestedProperty("identifications"))
            call.finishList()
        }

        val errors = call.finishObject()

        val result =
            if (errors != null) ValidationResult.invalid(errors)
            else ValidationResult.valid(person)
        return result
    }

    private fun validateIdentifications(
        identifications: List<PersonIdentification>,
        call: ValidationCall
    ) {
        checkNotEmpty("", identifications, call)
        for (identification in identifications) {
            val item = call.listItem()
            checkNotNull("__root__", identification, call)

            if (item != null) {
                validatePersonIdentification(identification, item)
            }

            item.finishObject()
        }
    }

    private fun validatePersonIdentification(
        identification: PersonIdentification,
        call: ValidationCall
    ) {
        checkNotNull("type", identification.type, call)
        checkNotNull("confidence", identification.confidence, call)
        if (identification.confidence != null) {
            checkMin("confidence", identification.confidence, 0, call)
            checkMax("confidence", identification.confidence, 100, call)
        }
    }

    private fun validateAddress(address: Address, call: ValidationCall) {
        checkNotNull("addressLine1", address.addressLine1, call)
        if (address.addressLine1 != null) {
            checkNotBlank("addressLine1", address.addressLine1, call)
        }
        checkNotNull("addressLine2", address.addressLine2, call)
        if (address.addressLine2 != null) {
            checkNotBlank("addressLine2", address.addressLine2, call)
        }
    }
}

private fun checkNotNull(prop: String, data: Any?, call: ValidationCall) {
    if (data == null) {
        call.propertyError(prop, SingleConstraintError("NotNull"))
    }
}

private fun checkNotEmpty(prop: String, data: Collection<Any>, call: ValidationCall) {
    if (data.isEmpty()) {
        if (prop.isEmpty()) {
            call.directError(SingleConstraintError("NotEmpty"))
        } else {
            call.propertyError(prop, SingleConstraintError("NotEmpty"))
        }
    }
}

private fun checkNotEmpty(prop: String, data: String, call: ValidationCall) {
    if (data.isEmpty()) {
        call.propertyError(prop, SingleConstraintError("NotEmpty"))
    }
}

private fun checkNotBlank(prop: String, data: String, call: ValidationCall) {
    if (data.isBlank()) {
        call.propertyError(prop, SingleConstraintError("NotBlank"))
    }
}

private fun checkMin(prop: String, confidence: Double, min: Int, call: ValidationCall) {
    if (confidence < min) {
        call.propertyError(prop, SingleConstraintError("Min", "Should be greater than $min"))
    }
}

private fun checkMax(prop: String, confidence: Double, max: Int, call: ValidationCall) {
    if (confidence > max) {
        call.propertyError(prop, SingleConstraintError("Max", "Should be less than $max"))
    }
}
