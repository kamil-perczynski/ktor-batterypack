package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.ksp.codegen.CodegenModelFixture.somePersonValidatorModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ValidatorCodegenTest {

    private val validatorCodegen = ValidatorCodegen()

    @Test
    @Disabled
    fun testGenerateValidatorClass() {
        val validatorModel = somePersonValidatorModel()

        val clazz = validatorCodegen.generateValidatorClass(validatorModel)

        assertThat(clazz).isEqualTo(
            """
               package io.github.ktor_batterypack.validation.example

               import io.github.ktor_batterypack.validation.example.PersonValidator
               import io.github.ktor_batterypack.validation.model.Address
               import io.github.ktor_batterypack.validation.model.Person
               import io.github.ktor_batterypack.validation.model.PersonIdentification
               import io.github.ktor_batterypack.validation.model.PersonIdentificationType
               import io.github.ktor_batterypack.validation.ValidationResult
               import io.github.ktor_batterypack.validation.SingleConstraintError
               import io.github.ktor_batterypack.validation.Constraints
               import io.github.ktor_batterypack.validation.ValidationCall

               import javax.annotation.processing.Generated

               @Generated
               @Suppress("SENSELESS_COMPARISON")
               class PersonValidatorImpl : PersonValidator {

                   override fun validate(person: Person): ValidationResult<Person> {
                       val call = ValidationCall()
                       Constraints.checkNotNull("firstName", person.firstName, call)
                       if (person.firstName != null) {
                           Constraints.checkNotBlank("firstName", person.firstName, call)
                       }
                       Constraints.checkNotNull("lastName", person.lastName, call)
                       if (person.lastName != null) {
                           Constraints.checkNotBlank("lastName", person.lastName, call)
                           Constraints.checkSize("lastName", person.lastName, call, min=2, max=50, )
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
                       if (address.addressLine1 != null) {
                           Constraints.checkNotBlank("addressLine1", address.addressLine1, call)
                       }
                       Constraints.checkNotNull("addressLine2", address.addressLine2, call)
                       if (address.addressLine2 != null) {
                           Constraints.checkNotBlank("addressLine2", address.addressLine2, call)
                       }
                   }

                   private fun validatePersonIdentification(identification: PersonIdentification?, call: ValidationCall) {
                       if (identification == null) return
                       
                       Constraints.checkNotNull("type", identification.type, call)
                       Constraints.checkNotNull("confidence", identification.confidence, call)
                       if (identification.confidence != null) {
                           Constraints.checkSize("confidence", identification.confidence, call, min=0, max=100, )
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

            """.trimIndent()
        )
    }
}
