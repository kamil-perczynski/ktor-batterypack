package io.github.ktor_batterypack.validation.ksp

import io.github.ktor_batterypack.validation.example.AppPropsValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator
import io.github.ktor_batterypack.validation.example.JsonPersonValidator2
import io.github.ktor_batterypack.validation.invoice.InvoiceCreateValidator
import io.github.ktor_batterypack.validation.reflection.ReflectionValidatorInterface
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.tuple
import org.junit.jupiter.api.Test

class CodegenModelResolverTest {

    val resolver = CodegenModelResolver(
        namingConvention = DefaultCodegenNamingConvention.DEFAULT_CODEGEN_NAMING_CONVENTION
    )

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
                tuple(false, "validatePersonIdentificationsList", "List<PersonIdentification>")
            )
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
                            Constraints.checkNotEmpty("modules", it, itemCall)
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
                
            """.trimIndent()
        )
    }
}