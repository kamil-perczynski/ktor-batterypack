package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.codegen.emit.ValidatorEmitter
import io.github.ktor_batterypack.validation.example.AppPropsValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator2
import io.github.ktor_batterypack.validation.invoice.InvoiceCreateValidator
import io.github.ktor_batterypack.validation.reflection.ReflectionValidatorInterface
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class CodegenModelResolverTest {

    val resolver = CodegenModelResolver()

    @Test
    fun `json validator schedules overridden and generated methods`() {
        val model = resolve(JsonPersonValidator::class, CodegenMode.JSON)

        assertThat(model.entryMethods)
            .extracting({ it.name }, { it.value.kind }, { it.value.validateWith })
            .containsExactly(tuple("validatePerson", ValueKind.OBJECT, "validatePerson"))

        assertThat(model.typeMethods)
            .extracting({ it.name }, { it.declaredParamType != null }, { it.kind })
            .containsExactly(
                tuple("validateAddress", false, ValueKind.OBJECT),
                tuple("validateIdDocIdentification", true, ValueKind.OBJECT),
                tuple("validateLivenessIdentification", true, ValueKind.OBJECT),
                tuple("validatePerson", false, ValueKind.OBJECT),
                tuple("validatePersonCreditCardsList", false, ValueKind.LIST),
                tuple("validatePersonIdentificationsList", false, ValueKind.LIST)
            )
    }

    @Test
    fun `json validator checks enum properties inline`() {
        val model = resolve(JsonPersonValidator2::class, CodegenMode.JSON)

        assertThat(model.typeMethods)
            .extracting({ it.name }, { it.declaredParamType != null }, { it.kind })
            .containsExactly(
                tuple("validatePerson", false, ValueKind.OBJECT),
                tuple("validatePersonCreditCardsList", false, ValueKind.LIST),
                tuple("validatePersonIdentification", false, ValueKind.OBJECT),
                tuple("validatePersonIdentificationsList", false, ValueKind.LIST)
            )

        val type = model.typeMethods
            .single { method -> method.name == "validatePersonIdentification" }
            .properties
            .single { property -> property.name == "type" }

        assertThat(type.value.kind).isEqualTo(ValueKind.ENUM)
        assertThat(type.value.validateWith).isNull()
        assertThat(type.value.type.enumValues).containsExactly(
            "ID_DOCUMENT_CHECK",
            "LIVENESS_CHECK",
            "IN_PERSON_IDENTIFICATION",
            "SIGNATURE_SPECIMEN_1",
            "SIGNATURE_SPECIMEN_2"
        )
    }

    @Test
    fun `typed validator reuses methods for shared types`() {
        val model = resolve(InvoiceCreateValidator::class, CodegenMode.TYPED)

        assertThat(model.entryMethods)
            .extracting({ it.name }, { it.value.validateWith })
            .containsExactly(tuple("validateInvoiceCreate", "validateInvoiceCreate"))

        assertThat(model.typeMethods)
            .extracting({ it.name }, { it.declaredParamType != null }, { it.kind })
            .containsExactly(
                tuple("validateContractParty", false, ValueKind.OBJECT),
                tuple("validateInvoiceCreate", false, ValueKind.OBJECT),
                tuple("validateInvoiceCreateMeteringPointPositionsList", false, ValueKind.LIST),
                tuple("validateInvoiceCreatePositionsList", false, ValueKind.LIST),
                tuple("validateInvoicePosition", true, ValueKind.OBJECT),
                tuple("validateMeteringPointAddress", false, ValueKind.OBJECT),
                tuple("validateMeteringPointPosition", false, ValueKind.OBJECT),
                tuple("validateMeteringPointPositionPositionsList", false, ValueKind.LIST)
            )
    }

    @Test
    fun `typed validator emits nested objects, maps and lists`() {
        val model = resolve(AppPropsValidator::class, CodegenMode.TYPED)

        assertThat(ValidatorEmitter().emit(model).toString()).isEqualTo(
            """
                package io.github.ktor_batterypack.validation.example

                import io.github.ktor_batterypack.validation.Constraints
                import io.github.ktor_batterypack.validation.ValidationCall
                import io.github.ktor_batterypack.validation.ValidationResult
                import java.time.Clock
                import javax.`annotation`.processing.Generated
                import kotlin.String
                import kotlin.Suppress
                import kotlin.collections.List
                import kotlin.collections.Map

                /**
                 * Validator implementation for [AppPropsValidator].
                 *
                 * NOTE: This class is auto generated by Ktor-Batterypack-Validation (https://github.com/kamil-perczynski/ktor-batterypack).
                 * Do not edit the class manually.
                 */
                @Generated
                @Suppress("UNNECESSARY_SAFE_CALL")
                public class AppPropsValidatorImpl(
                    private val clock: Clock = Clock.systemDefaultZone(),
                ) : AppPropsValidator {
                    override fun validateAppProps(appProps: AppProps): ValidationResult<AppProps> {
                        val call = ValidationCall(clock = clock)
                        validateAppProps(appProps, call)
                        val errors = call.finishObject()
                        return if (errors != null) ValidationResult.invalid(appProps, errors) else ValidationResult.valid(appProps)
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
                            Constraints.checkNotEmpty("$", it, itemCall)
                            validateDatabasePropsConnectionPropsMap(it, itemCall)
                            itemCall.finishObject()
                        }
                    }

                    private fun validateDatabasePropsConnectionPropsMap(connectionProps: Map<String, DatabaseProps>?, call: ValidationCall) {
                        if (connectionProps == null) return
                        for ((key, item) in connectionProps) {
                            val itemCall = call.nestedProperty(key)
                            item?.let {
                                validateDatabaseProps(it, itemCall)
                            }
                            itemCall.finishObject()
                        }
                    }

                    private fun validateKtorProps(ktorProps: KtorProps?, call: ValidationCall) {
                        if (ktorProps == null) return
                        ktorProps.modules?.let {
                            val itemCall = call.nestedProperty("modules")
                            Constraints.checkNotEmpty("$", it, itemCall)
                            validateKtorPropsModulesList(it, itemCall)
                            itemCall.finishList()
                        }
                    }

                    private fun validateKtorPropsModulesList(modules: List<String>?, call: ValidationCall) {
                        if (modules == null) return
                        for (item in modules) {
                            val itemCall = call.listItem()
                            item?.let {
                                Constraints.checkNotBlank("$", it, itemCall)
                            }
                            itemCall.finishObject()
                        }
                    }
                }
            """.trimIndent() + "\n"
        )
    }

    @Test
    fun `json validator emits type checks before constraints`() {
        val model = resolve(JsonPersonValidator::class, CodegenMode.JSON)

        assertThat(ValidatorEmitter().emit(model).toString()).isEqualTo(
            """
                package io.github.ktor_batterypack.validation.example

                import io.github.ktor_batterypack.validation.Constraints
                import io.github.ktor_batterypack.validation.JsonConstraints
                import io.github.ktor_batterypack.validation.JsonTypeChecks
                import io.github.ktor_batterypack.validation.ValidationCall
                import io.github.ktor_batterypack.validation.ValidationResult
                import java.time.Clock
                import javax.`annotation`.processing.Generated
                import tools.jackson.databind.JsonNode
                import tools.jackson.databind.node.ArrayNode
                import tools.jackson.databind.node.ObjectNode

                /**
                 * Validator implementation for [JsonPersonValidator].
                 *
                 * NOTE: This class is auto generated by Ktor-Batterypack-Validation (https://github.com/kamil-perczynski/ktor-batterypack).
                 * Do not edit the class manually.
                 */
                @Generated
                public class JsonPersonValidatorImpl(
                    private val clock: Clock = Clock.systemDefaultZone(),
                ) : JsonPersonValidator {
                    override fun validatePerson(person: JsonNode): ValidationResult<JsonNode> {
                        val call = ValidationCall(clock = clock)
                        JsonTypeChecks.checkNotNull("$", person, call)
                        JsonTypeChecks.checkObject("$", person, call)?.let {
                            validatePerson(it, call)
                        }
                        val errors = call.finishObject()
                        return if (errors != null) ValidationResult.invalid(person, errors) else ValidationResult.valid(person)
                    }

                    private fun validateAddress(address: ObjectNode?, call: ValidationCall) {
                        if (address == null) return
                        JsonTypeChecks.checkNotNull("addressLine1", address, call)
                        JsonTypeChecks.checkString("addressLine1", address, call)
                        JsonTypeChecks.checkNotNull("addressLine2", address, call)
                        JsonTypeChecks.checkString("addressLine2", address, call)
                        JsonTypeChecks.checkNotNull("zipCode", address, call)
                        JsonTypeChecks.checkString("zipCode", address, call)?.let {
                            Constraints.checkNotBlank("zipCode", it, call)
                            Constraints.checkPattern("zipCode", it, call, regexp = "\\d{2}-\\d{3}")
                            JsonConstraints.checkSize("zipCode", it, call, min = 1, max = 100)
                        }
                        checkAddressLines(address, call)
                    }

                    override fun validateIdDocIdentification(idDocIdentification: ObjectNode?, call: ValidationCall) {
                        if (idDocIdentification == null) return
                        JsonTypeChecks.checkNotNull("idNumber", idDocIdentification, call)
                        JsonTypeChecks.checkString("idNumber", idDocIdentification, call)?.let {
                            Constraints.checkNotBlank("idNumber", it, call)
                        }
                        JsonTypeChecks.checkNotNull("issueDate", idDocIdentification, call)
                        JsonTypeChecks.checkString("issueDate", idDocIdentification, call)?.let {
                            Constraints.checkNotBlank("issueDate", it, call)
                        }
                        JsonTypeChecks.checkNotNull("type", idDocIdentification, call)
                        JsonTypeChecks.checkString("type", idDocIdentification, call)?.let {
                            JsonTypeChecks.checkEnum("type", it, call, setOf("ID_DOCUMENT_CHECK", "LIVENESS_CHECK", "IN_PERSON_IDENTIFICATION", "SIGNATURE_SPECIMEN_1", "SIGNATURE_SPECIMEN_2"))
                        }
                    }

                    override fun validateLivenessIdentification(livenessIdentification: ObjectNode?, call: ValidationCall) {
                        if (livenessIdentification == null) return
                        JsonTypeChecks.checkNotNull("confidence", livenessIdentification, call)
                        JsonTypeChecks.checkDouble("confidence", livenessIdentification, call)?.let {
                            Constraints.checkMin("confidence", it, call, `value` = 0)
                            Constraints.checkMax("confidence", it, call, `value` = 100)
                        }
                        JsonTypeChecks.checkNotNull("type", livenessIdentification, call)
                        JsonTypeChecks.checkString("type", livenessIdentification, call)?.let {
                            JsonTypeChecks.checkEnum("type", it, call, setOf("ID_DOCUMENT_CHECK", "LIVENESS_CHECK", "IN_PERSON_IDENTIFICATION", "SIGNATURE_SPECIMEN_1", "SIGNATURE_SPECIMEN_2"))
                        }
                    }

                    private fun validatePerson(person: ObjectNode?, call: ValidationCall) {
                        if (person == null) return
                        JsonTypeChecks.checkNotNull("birthDate", person, call)
                        JsonTypeChecks.checkLocalDate("birthDate", person, call)?.let {
                            Constraints.checkPast("birthDate", it, call)
                        }
                        JsonTypeChecks.checkNotNull("firstName", person, call)
                        JsonTypeChecks.checkString("firstName", person, call)?.let {
                            Constraints.checkNotNull("firstName", it, call)
                            Constraints.checkNotBlank("firstName", it, call)
                        }
                        JsonTypeChecks.checkNotNull("lastName", person, call)
                        JsonTypeChecks.checkString("lastName", person, call)?.let {
                            Constraints.checkNotNull("lastName", it, call)
                            Constraints.checkNotBlank("lastName", it, call)
                        }
                        JsonTypeChecks.checkNotNull("address", person, call)
                        JsonTypeChecks.checkObject("address", person, call)?.let {
                            val itemCall = call.nestedProperty("address")
                            validateAddress(it, itemCall)
                            itemCall.finishObject()
                        }
                        JsonTypeChecks.checkNotNull("creditCards", person, call)
                        JsonTypeChecks.checkArray("creditCards", person, call)?.let {
                            val itemCall = call.nestedProperty("creditCards")
                            JsonConstraints.checkNotEmpty("$", it, itemCall)
                            validatePersonCreditCardsList(it, itemCall)
                            itemCall.finishList()
                        }
                        JsonTypeChecks.checkNotNull("identifications", person, call)
                        JsonTypeChecks.checkArray("identifications", person, call)?.let {
                            val itemCall = call.nestedProperty("identifications")
                            JsonConstraints.checkNotEmpty("$", it, itemCall)
                            validatePersonIdentificationsList(it, itemCall)
                            itemCall.finishList()
                        }
                    }

                    private fun validatePersonCreditCardsList(creditCards: ArrayNode?, call: ValidationCall) {
                        if (creditCards == null) return
                        for (item in creditCards) {
                            val itemCall = call.listItem()
                            JsonTypeChecks.checkNotNull("$", item, itemCall)
                            JsonTypeChecks.checkString("$", item, itemCall)?.let {
                                Constraints.checkNotBlank("$", it, itemCall)
                                Constraints.checkPattern("$", it, itemCall, regexp = "^\\d{16}$")
                            }
                            itemCall.finishObject()
                        }
                    }

                    private fun validatePersonIdentificationsList(identifications: ArrayNode?, call: ValidationCall) {
                        if (identifications == null) return
                        for (item in identifications) {
                            val itemCall = call.listItem()
                            JsonTypeChecks.checkNotNull("$", item, itemCall)
                            JsonTypeChecks.checkObject("$", item, itemCall)?.let {
                                validatePersonIdentification(it, itemCall)
                            }
                            itemCall.finishObject()
                        }
                    }
                }
            """.trimIndent() + "\n"
        )
    }

    private fun resolve(validatorInterface: KClass<*>, mode: CodegenMode): ValidatorModel {
        val resolved = resolver.resolve(ReflectionValidatorInterface(validatorInterface), mode)

        assertThat(resolved.diagnostics).isEmpty()
        return resolved.model!!
    }
}
