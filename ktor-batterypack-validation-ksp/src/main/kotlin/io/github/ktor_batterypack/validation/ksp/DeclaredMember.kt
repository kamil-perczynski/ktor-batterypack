package io.github.ktor_batterypack.validation.ksp

interface DeclaredMember {
    val annotations: List<CodegenAnnotation>
    val name: String
    val type: CodegenType
    val hasDefault: Boolean
    val jsonName: String?
        get() {
            val jsonProperty = annotations.find { annotation -> annotation.fqName == JSON_PROPERTY_FQ_NAME }
            val value = jsonProperty?.args?.get("value") as? String
            return value?.takeIf { it.isNotEmpty() }
        }
    val isJsonIgnored: Boolean
        get() {
            val jsonIgnore = annotations.find { annotation -> annotation.fqName == JSON_IGNORE_FQ_NAME }
            return jsonIgnore != null && jsonIgnore.args["value"] != false
        }
}

interface CodegenAnnotation {
    val name: String
    val fqName: String
    val args: Map<String, Any>
}

private const val JSON_PROPERTY_FQ_NAME = "com.fasterxml.jackson.annotation.JsonProperty"
private const val JSON_IGNORE_FQ_NAME = "com.fasterxml.jackson.annotation.JsonIgnore"
