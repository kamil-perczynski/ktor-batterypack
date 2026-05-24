package io.github.kperczynski.libs.redis

/**
 * Data class representing information about a Redis consumer.
 *
 * @property name The name of the consumer.
 * @property pending The number of pending messages for this consumer.
 * @property idle The idle time in milliseconds since the last message was delivered to this consumer.
 */
data class RedisConsumerInfo(val name: String, val pending: Long, val idle: Long)

/**
 * Converts a raw list of consumer information (as returned by Redis)
 * into a list of [RedisConsumerInfo] objects.
 */
fun toXInfoResultDto(raw: List<*>): List<RedisConsumerInfo> {
    return raw.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val name = map["name"] as? String ?: return@mapNotNull null
        val pending = (map["pending"] as? Number)?.toLong() ?: return@mapNotNull null
        val idle = (map["idle"] as? Number)?.toLong() ?: return@mapNotNull null
        RedisConsumerInfo(name, pending, idle)
    }
}
