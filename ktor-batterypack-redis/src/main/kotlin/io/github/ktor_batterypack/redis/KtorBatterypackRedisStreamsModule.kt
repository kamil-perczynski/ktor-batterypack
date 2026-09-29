package io.github.ktor_batterypack.redis

import io.github.ktor_batterypack.core.di.InitCallback
import io.lettuce.core.RedisClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(KtorBatterypackRedisStreamsModule::class.java)

@Module(includes = [KtorBatterypackRedisModule::class])
class KtorBatterypackRedisStreamsModule {

    @Singleton(binds = [InitCallback::class, AutoCloseable::class])
    @Named("redisFetcher")
    fun redisFetcher(
        redisClient: RedisClient,
        @Provided redisProps: RedisProps,
        listeners: List<RedisStreamListener>,
        loops: List<RedisStreamsBackgroundLoop>,
    ): RedisStreamFetcher {
        val consumerGroup = redisProps.fetcher.consumerGroup
        val selected = listeners.filter { listener ->
            listener.config().consumerGroup == null || listener.config().consumerGroup == consumerGroup
        }
        val excluded = listeners - selected
        if (excluded.isNotEmpty()) {
            log.warn(
                "Excluded {} redis stream listener(s) whose consumer group does not match '{}': {}",
                excluded.size,
                consumerGroup,
                excluded.joinToString { "${it::class.simpleName} (${it.config().consumerGroup})" }
            )
        }
        return RedisStreamFetcher(
            consumerId = nextConsumerId(consumerGroup),
            redisClient = redisClient,
            consumerGroup = consumerGroup,
            autoclaimMinIdleMs = redisProps.fetcher.autoclaimMinIdleMs,
            loops = loops,
            listeners = selected,
        )
    }

}
