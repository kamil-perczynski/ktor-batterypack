package io.github.ktor_batterypack.core

import io.github.ktor_batterypack.core.di.InitCallback
import io.github.ktor_batterypack.core.di.KoinLifecycleListener
import io.github.ktor_batterypack.core.di.LifecycleListener
import org.koin.core.annotation.*
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Module
@ComponentScan("io.github.ktor_batterypack.core")
@Configuration
class KtorBatterypackCoreModule {

    @Singleton
    fun jsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
    }

    @Singleton(binds = [LifecycleListener::class])
    fun koinLifecycleListener(
        closeCallbacks: List<AutoCloseable>,
        initCallbacks: List<InitCallback>
    ): KoinLifecycleListener {
        return KoinLifecycleListener(closeCallbacks, initCallbacks)
    }

}
