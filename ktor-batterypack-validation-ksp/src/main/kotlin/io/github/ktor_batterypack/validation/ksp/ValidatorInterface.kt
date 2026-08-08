package io.github.ktor_batterypack.validation.ksp

interface ValidatorInterface {
    val name: String
    val memberFunctions: List<MemberFn>
    val fqName: String
}
