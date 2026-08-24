package io.github.ktor_batterypack.validation.ksp

interface DeclaredMember {
    val annotations: List<CodegenAnnotation>
    val name: String
    val type: CodegenType
    val itemType: CodegenType?
}

interface CodegenAnnotation {
    val name: String
    val fqName: String
    val args: Map<String, Any>
}