package io.github.ktor_batterypack.validation.ksp

import com.squareup.kotlinpoet.ClassName

interface ValidatorInterface {
    val name: String
    val className: ClassName
    val memberFunctions: List<MemberFn>
    val abstractProperties: List<ValidatorProperty>
    val fqName: String
    val symbol: Any?
}
