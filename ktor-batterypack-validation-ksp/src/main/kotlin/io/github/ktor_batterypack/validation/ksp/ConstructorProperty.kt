package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.TypeName

data class ConstructorProperty(
    val name: String,
    val typeName: TypeName
)
