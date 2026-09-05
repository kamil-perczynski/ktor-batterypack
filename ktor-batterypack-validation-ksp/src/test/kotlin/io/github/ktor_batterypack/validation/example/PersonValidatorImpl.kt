@file:Suppress("unused")

package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.Constraints
import io.github.ktor_batterypack.validation.ValidationCall
import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.model.Address
import io.github.ktor_batterypack.validation.model.Person
import io.github.ktor_batterypack.validation.model.PersonIdentification
import javax.annotation.processing.Generated

@Generated
@Suppress("UNNECESSARY_SAFE_CALL")
class PersonValidatorImpl : PersonValidator {

    override fun validate(person: Person): ValidationResult<Person> {
        val call = ValidationCall()
        validatePerson(person, call)
        val errors = call.finishObject()

        val result : ValidationResult<Person> =
            if (errors != null) ValidationResult.invalid(person, errors)
            else ValidationResult.valid(person)

        return result
    }

    private fun validateAddress(address: Address?, call: ValidationCall) {
        if (address == null) return
        
        address.zipCode?.let {
            Constraints.checkNotBlank("zipCode", it, call)
            Constraints.checkPattern("zipCode", it, call, regexp="\\d{2}-\\d{3}", )
        }
    }

    private fun validatePerson(person: Person?, call: ValidationCall) {
        if (person == null) return
        
        person.birthDate?.let {
            Constraints.checkPast("birthDate", it, call)
        }
        person.firstName?.let {
            Constraints.checkNotNull("firstName", it, call)
            Constraints.checkNotBlank("firstName", it, call)
        }
        person.lastName?.let {
            Constraints.checkNotNull("lastName", it, call)
            Constraints.checkNotBlank("lastName", it, call)
        }
        person.address?.let {
            val itemCall = call.nestedProperty("address")
            validateAddress(it, itemCall)
            itemCall.finishObject()
        }
        person.identifications?.let {
            val itemCall = call.nestedProperty("identifications")
            Constraints.checkNotEmpty(it, itemCall)
            validatePersonIdentificationsList(it, itemCall)
            itemCall.finishList()
        }
    }

    private fun validatePersonIdentification(personIdentification: PersonIdentification?, call: ValidationCall) {
        if (personIdentification == null) return
        
    }

    private fun validatePersonIdentificationsList(identifications: List<PersonIdentification>?, call: ValidationCall) {
        if (identifications == null) return
        
        for (item in identifications) {
            val itemCall = call.listItem()
            validatePersonIdentification(item, itemCall)
            itemCall.finishObject()
        }
    }

}
