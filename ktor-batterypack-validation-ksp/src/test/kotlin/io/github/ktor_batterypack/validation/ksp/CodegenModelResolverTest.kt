package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.example.AppPropsValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator2
import io.github.ktor_batterypack.validation.example.PersonValidator
import io.github.ktor_batterypack.validation.invoice.InvoiceCreateValidator
import io.github.ktor_batterypack.validation.reflection.ReflectionValidatorInterface
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.tuple
import org.junit.jupiter.api.Test

class CodegenModelResolverTest {

    val resolver = CodegenModelResolver()

    @Test
    fun testJsonValidatorWithOverriddenMethodsAndCustomChecks() {
        val iface = ReflectionValidatorInterface(JsonPersonValidator::class)
        val model = resolver.resolve(iface)

        assertThat(model.methods)
            .extracting({ it.name }, { it.param.type })
            .containsExactly(
                tuple("validatePerson", "Person")
            )

        assertThat(model.privateMethods)
            .extracting({ it.isOverride }, { it.name }, { it.param.type })
            .containsExactly(
                tuple(false, "validateAddress", "Address"),
                tuple(true, "validateIdDocIdentification", "IdDocIdentification"),
                tuple(true, "validateLivenessIdentification", "LivenessIdentification"),
                tuple(false, "validatePerson", "Person"),
                tuple(false, "validatePersonIdentificationType", "PersonIdentificationType"),
                tuple(false, "validatePersonIdentificationsList", "List<PersonIdentification>")
            )
    }

    @Test
    fun testBasicJsonValidatorWithCustomValidationMethod() {
        val iface = ReflectionValidatorInterface(JsonPersonValidator2::class)
        val model = resolver.resolve(iface)

        assertThat(model.methods)
            .extracting({ it.name }, { it.param.type })
            .containsExactly(
                tuple("validatePerson", "Person")
            )
        assertThat(model.privateMethods)
            .extracting({ it.isOverride }, { it.name }, { it.param.type })
            .containsExactly(
                tuple(false, "validatePerson", "Person"),
                tuple(false, "validatePersonIdentification", "PersonIdentification"),
                tuple(false, "validatePersonIdentificationType", "PersonIdentificationType"),
                tuple(false, "validatePersonIdentificationsList", "List<PersonIdentification>")
            )

        val validatePersonIdentification = model.privateMethods
            .find { it.name == "validatePersonIdentificationType" }

        assertThat(validatePersonIdentification).isNotNull

        val enumConstants = listOf(
            "ID_DOCUMENT_CHECK",
            "LIVENESS_CHECK",
            "IN_PERSON_IDENTIFICATION",
            "SIGNATURE_SPECIMEN_1",
            "SIGNATURE_SPECIMEN_2"
        )
        val param = validatePersonIdentification?.param
        assertThat(param?.name).isEqualTo("personIdentificationType")
        assertThat(param?.codegenType?.isEnum).isTrue
        assertThat(param?.codegenType?.enumValues).isEqualTo(enumConstants)
    }

    @Test
    fun testComplexValidatorWithOverriddenMethod() {
        val iface = ReflectionValidatorInterface(InvoiceCreateValidator::class)
        val model = resolver.resolve(iface)

        assertThat(model.methods)
            .extracting({ it.name }, { it.param.type })
            .containsExactly(
                tuple("validateInvoiceCreate", "InvoiceCreate")
            )

        assertThat(model.privateMethods)
            .extracting({ it.isOverride }, { it.name }, { it.param.type })
            .containsExactly(
                tuple(false, "validateBillingComponentUnit", "BillingComponentUnit"),
                tuple(false, "validateContractParty", "ContractParty"),
                tuple(false, "validateInvoiceCreate", "InvoiceCreate"),
                tuple(
                    false,
                    "validateInvoiceCreateMeteringPointPositionsList",
                    "List<MeteringPointPosition>"
                ),
                tuple(false, "validateInvoiceCreatePositionsList", "List<InvoicePosition>"),
                tuple(true, "validateInvoicePosition", "InvoicePosition"),
                tuple(false, "validateMeteringPointAddress", "MeteringPointAddress"),
                tuple(false, "validateMeteringPointPosition", "MeteringPointPosition"),
                tuple(false, "validateMeteringPointPositionPositionsList", "List<InvoicePosition>")
            )
    }

    @Test
    fun testGenerateBasicValidatorClass() {
        val iface = ReflectionValidatorInterface(AppPropsValidator::class)
        val model = resolver.resolve(iface)

        val generatedValidatorClass = ValidatorCodegen().generateValidatorClass(model)

        assertThat(generatedValidatorClass).isEqualTo(
            """
                @file:Suppress("unused")

                package io.github.ktor_batterypack.validation.example

                import io.github.ktor_batterypack.validation.ValidationResult
                import io.github.ktor_batterypack.validation.example.AppProps
                import io.github.ktor_batterypack.validation.example.DatabaseProps
                import io.github.ktor_batterypack.validation.example.KtorProps
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
                            if (errors != null) ValidationResult.invalid(appProps, errors)
                            else ValidationResult.valid(appProps)

                        return result
                    }

                    private fun validateAppProps(appProps: AppProps?, call: ValidationCall) {
                        if (appProps == null) return
                        
                        appProps.databaseProps?.let {
                            val itemCall = call.nestedProperty("databaseProps")
                            validateDatabaseProps(it, itemCall)
                            itemCall.finishObject()
                        }
                        appProps.ktor?.let {
                            val itemCall = call.nestedProperty("ktor")
                            validateKtorProps(it, itemCall)
                            itemCall.finishObject()
                        }
                    }

                    private fun validateDatabaseProps(databaseProps: DatabaseProps?, call: ValidationCall) {
                        if (databaseProps == null) return
                        
                        databaseProps.connectionProps?.let {
                            val itemCall = call.nestedProperty("connectionProps")
                            validateDatabasePropsConnectionPropsMap(it, itemCall)
                            itemCall.finishObject()
                        }
                    }

                    private fun validateDatabasePropsConnectionPropsMap(connectionProps: Map<String, DatabaseProps>?, call: ValidationCall) {
                        if (connectionProps == null) return
                        
                        for (entry in connectionProps.entries) {
                            val (key, value) = entry

                            val nested = call.nestedProperty(key)
                            validateDatabaseProps(value, nested)
                            nested.finishObject()
                        }
                    }

                    private fun validateKtorProps(ktorProps: KtorProps?, call: ValidationCall) {
                        if (ktorProps == null) return
                        
                        ktorProps.modules?.let {
                            val itemCall = call.nestedProperty("modules")
                            Constraints.checkNotEmpty(it, itemCall)
                            validateKtorPropsModulesList(it, itemCall)
                            itemCall.finishList()
                        }
                    }

                    private fun validateKtorPropsModulesList(modules: List<String>?, call: ValidationCall) {
                        if (modules == null) return
                        
                        for (item in modules) {
                            val itemCall = call.listItem()
                            item?.let {
                                Constraints.checkNotBlank(it, itemCall)
                            }
                            itemCall.finishObject()
                        }
                    }

                }
                
            """.trimIndent()
        )
    }

    @Test
    fun testGenerateBasicValidatorClassJson() {
        val iface = ReflectionValidatorInterface(JsonPersonValidator::class)
        val model = resolver.resolve(iface)

        val generatedValidatorClass = ValidatorCodegen().generateJsonValidatorClass(model)

        assertThat(generatedValidatorClass).isEqualTo(
            """
                @file:Suppress("unused")

                package io.github.ktor_batterypack.validation.example

                import io.github.ktor_batterypack.validation.ValidationCall
                import io.github.ktor_batterypack.validation.ValidationResult
                import io.github.ktor_batterypack.validation.Constraints
                import io.github.ktor_batterypack.validation.JsonConstraints
                import tools.jackson.databind.JsonNode
                import tools.jackson.databind.node.ArrayNode
                import tools.jackson.databind.node.ObjectNode
                import javax.annotation.processing.Generated


                @Generated
                @Suppress("UNNECESSARY_SAFE_CALL")
                class JsonPersonValidatorImpl : JsonPersonValidator {

                    override fun validatePerson(person: JsonNode): ValidationResult<JsonNode> {
                        val call = ValidationCall()
                        JsonConstraints.checkNotNull(person, call)
                        JsonConstraints.checkObject(person, call)?.let {
                            validatePerson(it, call)
                        }
                        val errors = call.finishObject()

                        val result : ValidationResult<JsonNode> =
                            if (errors != null) ValidationResult.invalid(person, errors)
                            else ValidationResult.valid(person)

                        return result
                    }

                    private fun validateAddress(address: ObjectNode?, call: ValidationCall) {
                        if (address == null) return
                        
                        JsonConstraints.checkNotNull("addressLine1", address, call)
                        JsonConstraints.checkString("addressLine1", address, call)?.let {
                        }

                        JsonConstraints.checkNotNull("addressLine2", address, call)
                        JsonConstraints.checkString("addressLine2", address, call)?.let {
                        }

                        JsonConstraints.checkNotNull("zipCode", address, call)
                        JsonConstraints.checkString("zipCode", address, call)?.let {
                            Constraints.checkNotBlank("zipCode", it, call)
                            Constraints.checkPattern("zipCode", it, call, regexp="\\d{2}-\\d{3}")
                            JsonConstraints.checkSize("zipCode", it, call, min=1, max=100)
                        }

                        checkAddressLines(address, call)
                    }

                    override fun validateIdDocIdentification(idDocIdentification: ObjectNode?, call: ValidationCall) {
                        if (idDocIdentification == null) return
                        
                        JsonConstraints.checkNotNull("idNumber", idDocIdentification, call)
                        JsonConstraints.checkString("idNumber", idDocIdentification, call)?.let {
                        }

                        JsonConstraints.checkNotNull("issueDate", idDocIdentification, call)
                        JsonConstraints.checkString("issueDate", idDocIdentification, call)?.let {
                        }

                        JsonConstraints.checkNotNull("type", idDocIdentification, call)
                        validatePersonIdentificationType(idDocIdentification, call, "type")
                    }

                    override fun validateLivenessIdentification(livenessIdentification: ObjectNode?, call: ValidationCall) {
                        if (livenessIdentification == null) return
                        
                        JsonConstraints.checkNotNull("confidence", livenessIdentification, call)
                        JsonConstraints.checkDouble("confidence", livenessIdentification, call)?.let {
                        }

                        JsonConstraints.checkNotNull("type", livenessIdentification, call)
                        validatePersonIdentificationType(livenessIdentification, call, "type")
                    }

                    private fun validatePerson(person: ObjectNode?, call: ValidationCall) {
                        if (person == null) return
                        
                        JsonConstraints.checkNotNull("birthDate", person, call)
                        JsonConstraints.checkLocalDate("birthDate", person, call)?.let {
                            Constraints.checkPast("birthDate", it, call)
                        }

                        JsonConstraints.checkNotNull("firstName", person, call)
                        JsonConstraints.checkString("firstName", person, call)?.let {
                            Constraints.checkNotBlank("firstName", it, call)
                        }

                        JsonConstraints.checkNotNull("lastName", person, call)
                        JsonConstraints.checkString("lastName", person, call)?.let {
                            Constraints.checkNotBlank("lastName", it, call)
                        }

                        JsonConstraints.checkNotNull("address", person, call)
                        JsonConstraints.checkObject("address", person, call)?.let {
                            val itemCall = call.nestedProperty("address")
                            validateAddress(it, itemCall)
                            itemCall.finishObject()
                        }
                        JsonConstraints.checkNotNull("identifications", person, call)
                        JsonConstraints.checkArray("identifications", person, call)?.let {
                            val itemCall = call.nestedProperty("identifications")
                            JsonConstraints.checkNotEmpty(it, itemCall)
                            validatePersonIdentificationsList(it, itemCall)
                            itemCall.finishList()
                        }
                    }

                    private fun validatePersonIdentificationType(personIdentificationType: JsonNode?, call: ValidationCall, prop: String?) {
                        if (personIdentificationType == null) return
                        
                        JsonConstraints.checkString(prop, personIdentificationType, call)?.let {
                            JsonConstraints.checkEnum(
                                prop = prop,
                                value = it,
                                call = call,
                                allowedValues = setOf(
                                    "ID_DOCUMENT_CHECK",
                                    "LIVENESS_CHECK",
                                    "IN_PERSON_IDENTIFICATION",
                                    "SIGNATURE_SPECIMEN_1",
                                    "SIGNATURE_SPECIMEN_2",
                                )
                            )
                        }
                    }

                    private fun validatePersonIdentificationsList(identifications: ArrayNode?, call: ValidationCall) {
                        if (identifications == null) return
                        
                        for (item in identifications) {
                            val itemCall = call.listItem()
                            JsonConstraints.checkNotNull(item, itemCall)
                            JsonConstraints.checkObject(item, itemCall)?.let {
                                validatePersonIdentification(it, itemCall)
                            }
                            
                            itemCall.finishObject()
                        }
                    }

                }
                
            """.trimIndent()
        )
    }
}
