package io.github.ktor_batterypack.validation

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ValidationParamType(val value: KClass<out Any>)
