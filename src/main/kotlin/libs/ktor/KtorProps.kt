package io.github.kperczynski.libs.ktor

data class KtorProps(
    val deployment: DeploymentProps = DeploymentProps(),
    val application: KtorApplicationProps = KtorApplicationProps()
)