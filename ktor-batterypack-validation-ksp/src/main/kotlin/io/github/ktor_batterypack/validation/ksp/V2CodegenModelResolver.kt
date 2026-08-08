package io.github.ktor_batterypack.validation.ksp

interface V2CodegenModelResolver {

    fun resolve(validatorInterface: ValidatorInterface) : CodegenModel

}