package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName

data class EntryMethod(
    val name: String,
    val paramName: String,
    val paramType: TypeName,
    val returnType: TypeName,
    val value: ValueModel,
    val isSuspend: Boolean
)
