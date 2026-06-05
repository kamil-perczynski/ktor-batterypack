package io.github.ktor_batterypack.core.ktor

import io.github.ktor_batterypack.core.multipart.MultipartProps

data class KtorProps(
    val deployment: DeploymentProps = DeploymentProps(),
    val application: KtorApplicationProps = KtorApplicationProps(),
    val multipart: MultipartProps = MultipartProps(),
    val banner: String? = null
)
