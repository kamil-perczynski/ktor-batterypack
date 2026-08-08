package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.example.AppPropsValidator
import io.github.ktor_batterypack.validation.reflection.ReflectionValidatorInterface
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class V2CodegenModelResolverImplTest {

    @Test
    fun generateClass() {
        val iface = ReflectionValidatorInterface(AppPropsValidator::class)
        val model = V2CodegenModelResolverImpl().resolve(iface)

        val generatedValidatorClass = ValidatorCodegen().generateValidatorClass(model)

        assertThat(generatedValidatorClass).isEqualTo(
            """
                @file:Suppress("unused")

                package io.github.ktor_batterypack.validation.example

                import io.github.ktor_batterypack.validation.ValidationResult
                import io.github.ktor_batterypack.validation.example.AppProps
                import io.github.ktor_batterypack.validation.example.DatabaseProps
                import io.github.ktor_batterypack.validation.model.Address
                import io.github.ktor_batterypack.validation.model.Person
                import io.github.ktor_batterypack.validation.model.PersonIdentification
                import kotlin.String
                import kotlin.collections.List
                import kotlin.collections.Map
                import io.github.ktor_batterypack.validation.SingleConstraintError
                import io.github.ktor_batterypack.validation.Constraints
                import io.github.ktor_batterypack.validation.ValidationCall

                import javax.annotation.processing.Generated

                @Generated
                @Suppress("UNNECESSARY_SAFE_CALL")
                class AppPropsValidatorImpl : AppPropsValidator {

                    override fun validateAppProps(appProps: AppProps): ValidationResult<AppProps> {
                        val call = ValidationCall()
                        validateAppProps(appProps, call)
                        val errors = call.finishObject()

                        val result : ValidationResult<AppProps> =
                            if (errors != null) ValidationResult.invalid(errors)
                            else ValidationResult.valid(appProps)

                        return result
                    }

                    override fun validatePerson(person: Person): ValidationResult<Person> {
                        val call = ValidationCall()
                        validatePerson(person, call)
                        val errors = call.finishObject()

                        val result : ValidationResult<Person> =
                            if (errors != null) ValidationResult.invalid(errors)
                            else ValidationResult.valid(person)

                        return result
                    }

                    private fun validateAppProps(appProps: AppProps?, call: ValidationCall) {
                        if (appProps == null) return
                        
                        appProps.databaseProps?.let {
                            val itemCall = call.nestedProperty("databaseProps")
                            validateDatabaseProps(it, itemCall)
                            itemCall.finishObject()
                        }
                        appProps.modules?.let {
                            val itemCall = call.nestedProperty("modules")
                            Constraints.checkNotEmpty("modules", it, itemCall)
                            validateModules(it, itemCall)
                            itemCall.finishList()
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
                            Constraints.checkNotEmpty("identifications", it, itemCall)
                            validateIdentifications(it, itemCall)
                            itemCall.finishList()
                        }
                    }

                    private fun validateDatabaseProps(databaseProps: DatabaseProps?, call: ValidationCall) {
                        if (databaseProps == null) return
                        
                        databaseProps.connectionProps?.let {
                            val itemCall = call.nestedProperty("connectionProps")
                            validateConnectionProps(it, itemCall)
                            itemCall.finishObject()
                        }
                    }

                    private fun validateModules(modules: List<String>?, call: ValidationCall) {
                        if (modules == null) return
                        
                        for (item in modules) {
                            val itemCall = call.listItem()
                            Constraints.checkNotNull("__root__", item, itemCall)
                            itemCall.finishObject()
                        }
                    }

                    private fun validateAddress(address: Address?, call: ValidationCall) {
                        if (address == null) return
                        
                        address.zipCode?.let {
                            Constraints.checkNotBlank("zipCode", it, call)
                            Constraints.checkPattern("zipCode", it, call, regexp="\\d{2}-\\d{3}", )
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

                    private fun validateConnectionProps(connectionProps: Map<String, DatabaseProps>?, call: ValidationCall) {
                        if (connectionProps == null) return
                        
                    }

                    private fun validatePersonIdentification(personIdentification: PersonIdentification?, call: ValidationCall) {
                        if (personIdentification == null) return
                        
                        personIdentification.confidence?.let {
                            Constraints.checkMin("confidence", it, call, value=0, )
                            Constraints.checkMax("confidence", it, call, value=100, )
                        }
                    }

                }

            """.trimIndent()
        )
    }
}