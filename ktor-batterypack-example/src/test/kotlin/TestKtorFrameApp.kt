package io.github.kperczynski

import io.github.kperczynski.infra.KtorFrameModule
import io.github.kperczynski.infra.oidc.OidcProps
import io.github.ktor_batterypack.authz.KtorBatterypackAuthzModule
import io.github.ktor_batterypack.core.KtorBatterypackCoreModule
import io.github.ktor_batterypack.database.KtorBatterypackDatabaseModule
import io.github.ktor_batterypack.metrics.KtorBatterypackMetricsModule
import io.github.ktor_batterypack.redis.KtorBatterypackRedisModule
import io.github.ktor_batterypack.redis_stream.KtorBatterypackRedisStreamsModule
import io.github.ktor_batterypack.redis.testing.TestRedisModule
import io.ktor.server.auth.oidc.OpenIdTestKeys
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@KoinApplication
object TestKtorFrameApp

@Module(
    includes = [
        KtorBatterypackCoreModule::class,
        KtorBatterypackDatabaseModule::class,
        KtorBatterypackMetricsModule::class,
        KtorBatterypackRedisModule::class,
        KtorBatterypackRedisStreamsModule::class,
        TestRedisModule::class,
        KtorBatterypackAuthzModule::class,
        KtorFrameModule::class
    ]
)
@Configuration
@ComponentScan("io.github.kperczynski")
class TestKtorFrameAppModule {

    @OptIn(ExperimentalKtorApi::class)
    @Singleton
    fun openIdTestKeys(props: OidcProps): OpenIdTestKeys {
        return OpenIdTestKeys.rsa(issuer = props.issuer, audience = props.audience.first())
    }
}
