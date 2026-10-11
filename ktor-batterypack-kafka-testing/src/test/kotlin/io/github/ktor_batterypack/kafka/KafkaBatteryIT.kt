package io.github.ktor_batterypack.kafka

import io.github.ktor_batterypack.kafka.testing.CCKafkaContainer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.testcontainers.containers.Network
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class KafkaBatteryIT {

    companion object {
        private val kafkaContainer = CCKafkaContainer()

        init {
            val network = Network.newNetwork()

            kafkaContainer
                .withNetwork(network)
                .start()
        }
    }

    @Test
    fun `should produce and consume round trip`() {
        val topic = "battery-it-${UUID.randomUUID()}"

        val producerProps = buildMap {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }

        KafkaProducer<String, String>(producerProps).use { producer ->
            producer.send(ProducerRecord(topic, "ping", "pong")).get()
        }

        val consumerProps = buildMap {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, "battery-it")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
        }

        KafkaConsumer<String, String>(consumerProps).use { consumer ->
            consumer.subscribe(listOf(topic))
            val records = consumer.poll(Duration.ofSeconds(30))

            assertEquals(1, records.count())
            val record = records.first()
            assertEquals("ping", record.key())
            assertEquals("pong", record.value())
        }
    }
}
