package io.github.ktor_batterypack.validation.ksp

data class ValidationModel(
    val packageName: String,
    val interfaceSimpleName: String,
    val implSimpleName: String,
    val imports: List<String>,
    val methods: List<MethodModel>
)

data class MethodModel(
    val name: String,
    val paramName: String,
    val dtoSimpleName: String,
    val properties: List<PropertyModel>
)

data class PropertyModel(
    val name: String,
    val typeName: String,
    val isString: Boolean,
    val isNumeric: Boolean,
    val isBoolean: Boolean,
    val isNullable: Boolean,
    val accessor: String,
    val nested: List<PropertyModel>
)
