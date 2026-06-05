package io.github.ktor_batterypack.core.ktor

data class KtorProps(
    val deployment: DeploymentProps = DeploymentProps(),
    val application: KtorApplicationProps = KtorApplicationProps()
)
