package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName

object PolymorphicTypes {

    private const val JSON_TYPE_NAME_FQ_NAME = "com.fasterxml.jackson.annotation.JsonTypeName"

    fun directSubtypes(type: CodegenType): List<CodegenSubtype> {
        val subtypes = linkedMapOf<String, CodegenSubtype>()
        val sealedSubtypes = type.sealedSubtypes
            .sortedBy { subtype -> subtype.fqName }
            .map { subtype -> CodegenSubtype(subtype, emptyList()) }
        for (subtype in type.jsonSubtypes + sealedSubtypes) {
            if (subtype.type.fqName != type.fqName) {
                subtypes.putIfAbsent(subtype.type.fqName, subtype)
            }
        }
        return subtypes.values.toList()
    }

    fun concreteSubtypes(type: CodegenType): List<CodegenSubtype> {
        val ownNames = type.jsonSubtypes.find { subtype -> subtype.type.fqName == type.fqName }?.names.orEmpty()
        val visited = linkedMapOf(type.fqName to CodegenSubtype(type, ownNames))
        val pending = ArrayDeque(listOf(type))
        while (pending.isNotEmpty()) {
            for (subtype in directSubtypes(pending.removeFirst())) {
                if (visited.putIfAbsent(subtype.type.fqName, subtype) == null) {
                    pending.addLast(subtype.type)
                }
            }
        }
        return visited.values.filter { subtype -> !subtype.type.isAbstract }
    }

    fun typeIds(subtype: CodegenSubtype, typeInfo: JsonTypeInfoModel): List<String> {
        if (subtype.names.isNotEmpty()) {
            return subtype.names
        }
        val typeName = subtype.type.declarationAnnotations
            .find { annotation -> annotation.fqName == JSON_TYPE_NAME_FQ_NAME }
            ?.args
            ?.get("value") as? String
        if (!typeName.isNullOrEmpty()) {
            return listOf(typeName)
        }
        return listOf(defaultTypeId(subtype.type, typeInfo))
    }

    private fun defaultTypeId(type: CodegenType, typeInfo: JsonTypeInfoModel): String {
        val className = when (val typeName = type.typeName.copy(nullable = false)) {
            is ClassName -> typeName
            is ParameterizedTypeName -> typeName.rawType
            else -> return type.name
        }
        if (typeInfo.use == "SIMPLE_NAME") {
            return className.simpleName
        }
        return className.simpleNames.joinToString("$")
    }
}
