package io.github.ktor_batterypack.validation.ksp

enum class MemberFnShape {
    VALIDATE_RESULT,
    VALIDATE_CALL,
    BUILT_IN,
    UNSUPPORTED;

    companion object {
        const val VALIDATION_RESULT_FQ_NAME = "io.github.ktor_batterypack.validation.ValidationResult"
        const val VALIDATION_CALL_FQ_NAME = "io.github.ktor_batterypack.validation.ValidationCall"

        private val BUILT_IN_NAMES = setOf("equals", "hashCode", "toString")

        fun of(function: MemberFn): MemberFnShape {
            if (function.name in BUILT_IN_NAMES) {
                return BUILT_IN
            }

            val params = function.params
            val returnTypeFqName = function.returnType?.fqName ?: "kotlin.Unit"
            if (params.size == 1 && returnTypeFqName == VALIDATION_RESULT_FQ_NAME) {
                return VALIDATE_RESULT
            }
            if (params.size == 2 && params[1].type.fqName == VALIDATION_CALL_FQ_NAME && returnTypeFqName == "kotlin.Unit") {
                return VALIDATE_CALL
            }
            return UNSUPPORTED
        }
    }
}
