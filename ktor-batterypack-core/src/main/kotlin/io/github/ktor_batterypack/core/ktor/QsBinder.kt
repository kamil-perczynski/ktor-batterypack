package io.github.ktor_batterypack.core.ktor

import io.ktor.http.Parameters
import io.ktor.util.flattenEntries
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ContainerNode
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ObjectNode

private val QS_SEGMENTS = compareBy<Pair<String, String>>(
    { (key, _) -> key.count { c -> c == '.' } },
    { (key, _) -> key }
)

object QsBinder {

    fun convert(params: Parameters): JsonNode {
        val root = JsonNodeFactory.instance.objectNode()

        val entries = params.flattenEntries().sortedWith(QS_SEGMENTS)

        for ((key, value) in entries) {
            val segments = key.split(".")

            require(segments.first().toIntOrNull() == null) {
                "Root-level query parameter key cannot be numeric: $key"
            }

            bind(root, segments, value)
        }
        return root
    }

    private fun bind(current: ContainerNode<*>, segments: List<String>, value: String) {
        if (segments.size == 1) {
            setLeaf(current, segments.first(), value)
            return
        }
        val child = getOrCreateContainer(current, segments.first(), segments[1])
        bind(child, segments.drop(1), value)
    }

    private fun setLeaf(current: ContainerNode<*>, segment: String, value: String) {
        when (current) {
            is ObjectNode -> {
                val existing = current.get(segment)
                when {
                    existing == null -> current.put(segment, value)
                    existing.isArray -> (existing as ArrayNode).add(value)
                    else -> current.replace(
                        segment,
                        JsonNodeFactory.instance.arrayNode().add(existing).add(value)
                    )
                }
            }

            is ArrayNode -> {
                val index = segment.toInt()
                val existing = current.get(index)
                while (current.size() <= index) current.addNull()
                when {
                    existing == null || existing.isNull ->
                        current.set(index, JsonNodeFactory.instance.stringNode(value))

                    existing.isArray -> (existing as ArrayNode).add(value)
                    else -> current.set(
                        index,
                        JsonNodeFactory.instance.arrayNode().add(existing).add(value)
                    )
                }
            }
        }
    }

    private fun getOrCreateContainer(
        current: ContainerNode<*>,
        segment: String,
        nextSegment: String
    ): ContainerNode<*> {
        val nextIsArray = nextSegment.toIntOrNull() != null
        return when (current) {
            is ObjectNode -> {
                val existing = current.get(segment)
                when {
                    existing is ContainerNode<*> -> existing
                    nextIsArray -> current.putArray(segment)
                    else -> current.putObject(segment)
                }
            }

            is ArrayNode -> {
                val index = segment.toInt()
                val existing = current.get(index)
                if (existing is ContainerNode<*>) return existing
                while (current.size() <= index) current.addNull()
                if (nextIsArray) {
                    JsonNodeFactory.instance.arrayNode().also { current.set(index, it) }
                } else {
                    JsonNodeFactory.instance.objectNode().also { current.set(index, it) }
                }
            }

            else -> error("Unreachable")
        }
    }

}
