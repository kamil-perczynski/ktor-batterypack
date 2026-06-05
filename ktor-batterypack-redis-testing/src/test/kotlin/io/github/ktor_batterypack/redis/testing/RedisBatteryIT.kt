package io.github.ktor_batterypack.redis.testing

import io.github.ktor_batterypack.core.configureKtorServer
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.plugin.module.dsl.withConfiguration

open class RedisBatteryIT {

    companion object {
        private val redisContainer = RedisTestContainer()
        internal var application: Application

        init {
            redisContainer.start()

            System.setProperty("config.override.redis.url", redisContainer.redisUri)

            val builder = ApplicationTestBuilder()
            builder.application {
                val ktorApp = this
                configureKtorServer { profiles ->
                    modules(
                        module {
                            single { ktorApp }
                        }
                    )
                    withConfiguration<TestRedisApp>()
                }
            }

            application = builder.application

            runBlocking {
                builder.startApplication()
            }
        }
    }

}
