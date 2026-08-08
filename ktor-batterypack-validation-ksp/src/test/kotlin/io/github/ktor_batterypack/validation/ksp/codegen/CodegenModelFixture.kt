package io.github.ktor_batterypack.validation.ksp.codegen

import io.github.ktor_batterypack.validation.ksp.CodegenModel
import io.github.ktor_batterypack.validation.ksp.ConstraintMethod
import io.github.ktor_batterypack.validation.ksp.DirectProperties
import io.github.ktor_batterypack.validation.ksp.NestedProperty
import io.github.ktor_batterypack.validation.ksp.PrivateValidationMethod
import io.github.ktor_batterypack.validation.ksp.PublicValidationMethod
import io.github.ktor_batterypack.validation.ksp.ValidationParameter

object CodegenModelFixture {

    fun somePersonValidatorModel(): CodegenModel = CodegenModel(
        imports = listOf(
            "io.github.ktor_batterypack.validation.example.PersonValidator",
            "io.github.ktor_batterypack.validation.model.Address",
            "io.github.ktor_batterypack.validation.model.Person",
            "io.github.ktor_batterypack.validation.model.PersonIdentification",
            "io.github.ktor_batterypack.validation.model.PersonIdentificationType",
            "io.github.ktor_batterypack.validation.ValidationResult"
        ),
        packageName = "io.github.ktor_batterypack.validation.example",
        interfaceName = "PersonValidator",
        fqInterfaceName = "io.github.ktor_batterypack.validation.example.PersonValidator",
        typeName = "PersonValidatorImpl",
        fqTypeName = "io.github.ktor_batterypack.validation.example.PersonValidatorImpl",
        methods = listOf(
            PublicValidationMethod(
                name = "validate",
                returnType = "ValidationResult<Person>",
                param = ValidationParameter(
                    name = "person",
                    type = "Person",
                    fqName = "io.github.ktor_batterypack.validation.model.Person",
                    isObject = true
                ),
                validationMethodName = "validatePerson",
            )
        ),
        privateMethods = listOf(
            PrivateValidationMethod(
                name = "validateAddress",
                param = ValidationParameter(
                    name = "address",
                    type = "Address",
                    fqName = "io.github.ktor_batterypack.validation.model.Address",
                    isObject = true,
                    isList = false
                ),
                directProperties = listOf(
                    DirectProperties(
                        name = "addressLine1",
                        type = "kotlin.String",
                        nullable = false,
                        constraints = mapOf(
                            "NotBlank" to ConstraintMethod("checkNotBlank"),
                        )
                    ),
                    DirectProperties(
                        name = "addressLine2",
                        type = "kotlin.String",
                        nullable = false,
                        constraints = mapOf(
                            "NotBlank" to ConstraintMethod("checkNotBlank"),
                        )
                    )
                ),
                nestedProperties = emptyList()
            ),
            PrivateValidationMethod(
                name = "validatePersonIdentification",
                param = ValidationParameter(
                    name = "identification",
                    type = "PersonIdentification",
                    fqName = "io.github.ktor_batterypack.validation.model.PersonIdentification",
                    isObject = true
                ),
                directProperties = listOf(
                    DirectProperties(
                        name = "type",
                        type = "io.github.ktor_batterypack.validation.model.PersonIdentificationType",
                        nullable = false,
                        constraints = emptyMap()
                    ),
                    DirectProperties(
                        name = "confidence",
                        type = "kotlin.Double",
                        nullable = false,
                        constraints = mapOf(
                            "Size" to ConstraintMethod(
                                "checkSize",
                                args = mapOf("min" to 0, "max" to 100)
                            ),
                        )
                    )
                ),
                nestedProperties = emptyList()
            ),
            PrivateValidationMethod(
                name = "validateIdentifications",
                param = ValidationParameter(
                    name = "identifications",
                    type = "List<PersonIdentification>",
                    fqName = "kotlin.collections.List",
                    isObject = false,
                    isList = true,
                    _itemValidatorMethodName = "validatePersonIdentification"
                ),
                directProperties = emptyList(),
                nestedProperties = emptyList()
            )
        )
    )
}
