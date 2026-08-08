package io.github.ktor_batterypack.validation.ksp

interface DeclaredMember {
    val constraints: List<DeclaredConstraint>
    val name: String
    val type: CodegenType
    val itemType: CodegenType?
}

interface DeclaredConstraint {
    val name: String
    val fqName: String
    val args: Map<String, Any>
}