package io.github.ktor_batterypack.validation.ksp

import com.google.common.graph.Graph

class GuavaMethodsGraph(private val graph: Graph<CodegenNode>) : MethodsGraph {

    override fun predecessors(node: CodegenNode): Set<CodegenNode> {
        return graph.predecessors(node)
    }

    override fun successors(node: CodegenNode): Set<CodegenNode> {
        return graph.successors(node)
    }

    override fun nodes(): Set<CodegenNode> {
        return graph.nodes()
    }
}