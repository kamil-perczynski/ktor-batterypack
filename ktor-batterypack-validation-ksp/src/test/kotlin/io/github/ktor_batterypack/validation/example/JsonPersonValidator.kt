package io.github.ktor_batterypack.validation.example

import io.github.ktor_batterypack.validation.*
import io.github.ktor_batterypack.validation.model.*
import io.github.ktor_batterypack.validation.model.PersonIdentificationType.ID_DOCUMENT_CHECK
import io.github.ktor_batterypack.validation.model.PersonIdentificationType.LIVENESS_CHECK
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

@Suppress("unused")
interface JsonPersonValidator {

    @ValidationParamType(Person::class)
    fun validatePerson(person: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(PersonIdentification::class)
    fun validatePersonIdentification(personIdentification: ObjectNode?, call: ValidationCall) {
        if (personIdentification == null) return

        when (val type = JsonTypeChecks.checkString("type", personIdentification)) {
            ID_DOCUMENT_CHECK.name -> validateIdDocIdentification(personIdentification, call)
            LIVENESS_CHECK.name -> validateLivenessIdentification(personIdentification, call)
            else -> call.propertyError(
                "type",
                SingleConstraintError("EnumConstant", "Unknown enum constant: '$type'")
            )
        }
    }

    @ValidationParamType(IdDocIdentification::class)
    fun validateIdDocIdentification(idDocIdentification: ObjectNode?, call: ValidationCall)

    @ValidationParamType(LivenessIdentification::class)
    fun validateLivenessIdentification(livenessIdentification: ObjectNode?, call: ValidationCall)

    @ValidationParamType(Address::class)
    fun checkAddressLines(address: ObjectNode?, call: ValidationCall) {
        val line1 = address?.get("addressLine1")?.asString(null) ?: return
        val line2 = address.get("addressLine2")?.asString(null) ?: return

        if (line1.isNotBlank() && line2.isNotBlank() && line1 == line2) {
            call.propertyError(
                "addressLine1",
                SingleConstraintError("AddressLineUnique", "Address lines must be unique")
            )
        }
    }

}

@Suppress("unused")
interface JsonPersonValidator2 {

    @ValidationParamType(Person::class)
    fun validatePerson(person: JsonNode): ValidationResult<JsonNode>

    @ValidationParamType(Address::class)
    fun validateAddress(address: ObjectNode?, call: ValidationCall) {

    }
}
