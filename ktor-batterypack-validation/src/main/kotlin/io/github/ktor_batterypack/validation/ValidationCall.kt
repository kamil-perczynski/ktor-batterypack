package io.github.ktor_batterypack.validation

import java.time.Clock

class ValidationCall(
    private val parent: ValidationCall? = null,
    private val parentProp: String? = null,
    private val listItem: Boolean = false,
    val clock: Clock = Clock.systemDefaultZone()
) {
    private val errors: MutableList<SingleConstraintError> = mutableListOf()
    private val items: MutableList<ConstraintError?> = mutableListOf()
    private val properties: MutableMap<String, MutableList<SingleConstraintError>> = mutableMapOf()
    private val nested: MutableMap<String, ConstraintError> = mutableMapOf()
    private val keyOrder: MutableSet<String> = linkedSetOf()

    fun propertyError(prop: String, constraintError: SingleConstraintError): ValidationCall {
        if (prop == "$") {
            directError(constraintError)
            return this
        }

        keyOrder.add(prop)
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
        return ValidationCall(this, prop, clock = clock)
    }

    fun listItem(): ValidationCall {
        return ValidationCall(this, null, true, clock)
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
            val next = LinkedHashMap<String, ConstraintError>()
            for (key in keyOrder) {
                val fieldErrors = properties[key]
                next[key] = if (fieldErrors != null) FieldConstraintError(fieldErrors) else nested.getValue(key)
            }
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
        keyOrder.add(prop)
        nested[prop] = constraintError
        return this
    }

}
