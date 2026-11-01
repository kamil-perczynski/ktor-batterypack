package io.github.ktor_batterypack.validation.ksp

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.PropertyNamingStrategy

class JsonNamingStrategy private constructor(private val strategy: PropertyNamingStrategy?) {

    companion object {
        const val OPTION = "ktor.validation.json.naming"

        private val STRATEGIES = mapOf(
            "LOWER_CAMEL_CASE" to PropertyNamingStrategies.LOWER_CAMEL_CASE,
            "UPPER_CAMEL_CASE" to PropertyNamingStrategies.UPPER_CAMEL_CASE,
            "SNAKE_CASE" to PropertyNamingStrategies.SNAKE_CASE,
            "UPPER_SNAKE_CASE" to PropertyNamingStrategies.UPPER_SNAKE_CASE,
            "LOWER_CASE" to PropertyNamingStrategies.LOWER_CASE,
            "KEBAB_CASE" to PropertyNamingStrategies.KEBAB_CASE,
            "LOWER_DOT_CASE" to PropertyNamingStrategies.LOWER_DOT_CASE,
        )

        val IDENTITY = JsonNamingStrategy(null)

        fun fromOption(value: String?): JsonNamingStrategy {
            if (value.isNullOrBlank()) {
                return IDENTITY
            }

            val strategy = STRATEGIES[value.trim().uppercase()]
                ?: throw IllegalArgumentException("Unsupported $OPTION '$value'. Supported values: ${STRATEGIES.keys}")

            return JsonNamingStrategy(strategy)
        }
    }

    fun translate(member: DeclaredMember): String {
        val explicitName = member.jsonName
        if (explicitName != null) {
            return explicitName
        }
        if (strategy == null) {
            return member.name
        }
        return strategy.nameForField(null, null, member.name)
    }
}
