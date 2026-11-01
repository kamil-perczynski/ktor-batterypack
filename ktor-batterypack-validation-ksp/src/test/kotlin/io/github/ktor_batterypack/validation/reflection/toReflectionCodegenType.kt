package io.github.ktor_batterypack.validation.reflection

import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

fun toReflectionCodegenType(type: KType): ReflectionCodegenType {
    return ReflectionCodegenType(
        kclass = type.jvmErasure,
        ktype = type,
    )
}