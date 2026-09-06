package io.github.ktor_batterypack.validation.ksp

interface MethodsGraph {
    fun predecessors(node: CodegenNode): Set<CodegenNode>
    fun nodes(): Set<CodegenNode>
    fun successors(node: CodegenNode): Set<CodegenNode>
}
