package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.Constraints
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.model.Address
import io.github.ktor_batterypack.validation.model.Person
import io.github.ktor_batterypack.validation.model.PersonIdentification
import javax.annotation.processing.Generated

@Suppress("SENSELESS_COMPARISON")
@Generated
class GeneratedPersonValidatorImpl : PersonValidator {

    override fun validate(person: Person): ValidationResult<Person> {
        val call = ValidationCall()
        if (person.firstName != null) {
            Constraints.checkNotBlank("firstName", person.firstName, call)
        }
        if (person.lastName != null) {
            Constraints.checkNotBlank("lastName", person.lastName, call)
        }
        Constraints.checkNotNull("birthDate", person.birthDate, call)
        if (person.birthDate != null) {
            Constraints.checkPast("birthDate", person.birthDate, call)
        }
        Constraints.checkNotNull("address", person.address, call)
        if (person.address != null) {
            val itemCall = call.nestedProperty("address")
            validateAddress(person.address, itemCall)
            itemCall.finishObject()
        }
        Constraints.checkNotNull("identifications", person.identifications, call)
        if (person.identifications != null) {
            val itemCall = call.nestedProperty("identifications")
            Constraints.checkNotEmpty("identifications", person.identifications, itemCall)
            validateIdentifications(person.identifications, itemCall)
            itemCall.finishList()
        }

        val errors = call.finishObject()
        val result : ValidationResult<Person> =
            if (errors != null) ValidationResult.invalid(errors)
            else ValidationResult.valid(person)

        return result
    }

    private fun validateAddress(address: Address?, call: ValidationCall) {
        if (address == null) return

        Constraints.checkNotNull("addressLine1", address.addressLine1, call)
        Constraints.checkNotNull("addressLine2", address.addressLine2, call)
        Constraints.checkNotNull("zipCode", address.zipCode, call)
        if (address.zipCode != null) {
            Constraints.checkNotBlank("zipCode", address.zipCode, call)
            Constraints.checkPattern("zipCode", address.zipCode, call, regexp="\\d{2}-\\d{3}")
        }
    }

    private fun validatePersonIdentification(personIdentification: PersonIdentification?, call: ValidationCall) {
        if (personIdentification == null) return

        Constraints.checkNotNull("type", personIdentification.type, call)
        Constraints.checkNotNull("confidence", personIdentification.confidence, call)
        if (personIdentification.confidence != null) {
            Constraints.checkMin("confidence", personIdentification.confidence, call, value=0)
            Constraints.checkMax("confidence", personIdentification.confidence, call, value=100)
        }
    }

    private fun validateIdentifications(identifications: List<PersonIdentification>?, call: ValidationCall) {
        if (identifications == null) return

        for (item in identifications) {
            val itemCall = call.listItem()
            Constraints.checkNotNull("__root__", item, itemCall)

            validatePersonIdentification(item, itemCall)
            itemCall.finishObject()
        }
    }

}