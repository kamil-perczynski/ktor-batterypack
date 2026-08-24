package io.github.ktor_batterypack.validation

class ValidationCall(
    private val parent: ValidationCall? = null,
    private val parentProp: String? = null,
    private val listItem: Boolean = false
) {
    private val errors: MutableList<SingleConstraintError> = mutableListOf()
    private val items: MutableList<ConstraintError?> = mutableListOf()
    private val properties: MutableMap<String, MutableList<SingleConstraintError>> = mutableMapOf()
    private val nested: MutableMap<String, ConstraintError> = mutableMapOf()

    fun propertyError(prop: String, constraintError: SingleConstraintError): ValidationCall {
        val errors = properties.computeIfAbsent(prop) { mutableListOf() }
        errors.add(constraintError)
        return this
    }

    fun itemErrors(error: ConstraintError?): ValidationCall {
        items.add(error)
        return this
    }

    fun directError(error: SingleConstraintError): ValidationCall {
        errors.add(error)
        return this
    }

    fun nestedProperty(prop: String): ValidationCall {
        return ValidationCall(this, prop)
    }

    fun listItem(): ValidationCall {
        return ValidationCall(this, null, true)
    }

    fun finishList(): ListConstraintError? {
        val constraintError =
            if (errors.isEmpty() && items.filterNotNull().isEmpty()) null
            else ListConstraintError(errors, items)

        if (parent != null && parentProp != null && constraintError != null) {
            parent.registerNestedObject(parentProp, constraintError)
        } else if (parent != null && listItem) {
            parent.itemErrors(constraintError)
        }

        return constraintError
    }

    fun finishObject(): ObjectConstraintError? {
        val constraintError = if (nested.isEmpty() && properties.isEmpty() && errors.isEmpty()) {
            null
        }
        else {
            val next = HashMap(nested)
            next.putAll(properties.mapValues { (_, fieldErrors) -> FieldConstraintError(fieldErrors) })
            if (errors.isNotEmpty()) {
                next["$"] = FieldConstraintError(errors)
            }
            ObjectConstraintError(next)
        }

        if (parent != null && parentProp != null && constraintError != null) {
            parent.registerNestedObject(parentProp, constraintError)
        } else if (parent != null && listItem) {
            parent.itemErrors(constraintError)
        }

        return constraintError
    }

    fun registerNestedObject(prop: String, constraintError: ConstraintError): ValidationCall {
        nested[prop] = constraintError
        return this
    }

}
