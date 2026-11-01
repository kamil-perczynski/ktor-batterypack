package io.github.ktor_batterypack.validation.ksp

data class JsonTypeInfoModel(
    val use: String,
    val include: String,
    val property: String
) {

    companion object {
        private const val JSON_TYPE_INFO_FQ_NAME = "com.fasterxml.jackson.annotation.JsonTypeInfo"
        private const val DEFAULT_TYPE_ID_PROPERTY = "@type"

        private val SUPPORTED_USES = setOf("NAME", "SIMPLE_NAME")
        private val SUPPORTED_INCLUDES = setOf("PROPERTY", "EXISTING_PROPERTY")

        fun of(type: CodegenType): JsonTypeInfoModel? {
            val annotation = (listOf(type) + type.supertypes).firstNotNullOfOrNull { candidate ->
                candidate.declarationAnnotations.find { annotation -> annotation.fqName == JSON_TYPE_INFO_FQ_NAME }
            } ?: return null

            return JsonTypeInfoModel(
                use = enumEntryName(annotation.args["use"]) ?: "NONE",
                include = enumEntryName(annotation.args["include"]) ?: "PROPERTY",
                property = annotation.args["property"] as? String ?: ""
            )
        }

        private fun enumEntryName(value: Any?): String? {
            return (value as? AnnotationValue.EnumEntry)?.entryName
        }
    }

    val isDisabled: Boolean
        get() = use == "NONE"

    val isSupported: Boolean
        get() = use in SUPPORTED_USES && include in SUPPORTED_INCLUDES

    val typeIdProperty: String
        get() = property.ifEmpty { DEFAULT_TYPE_ID_PROPERTY }
}
