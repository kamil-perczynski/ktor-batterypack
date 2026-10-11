package io.github.ktor_batterypack.kafka.testing

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.serializers.subject.DefaultReferenceSubjectNameStrategy
import io.confluent.kafka.serializers.subject.strategy.ReferenceSubjectNameStrategy

class PrefixSubjectNameStrategy : ReferenceSubjectNameStrategy {

    private val refDelegate = DefaultReferenceSubjectNameStrategy()

    private var prefix: String = ""

    override fun configure(configs: Map<String, *>) {
        if (configs.containsKey("schema.subject.name.prefix")) {
            val value = configs["schema.subject.name.prefix"]
            if (value is String) {
                prefix = value
            }
        }

        return refDelegate.configure(configs)
    }

    override fun subjectName(
        refName: String?,
        topic: String?,
        isKey: Boolean,
        schema: ParsedSchema?
    ): String? {
        if (prefix.isNotBlank()) {
            return prefix + refDelegate.subjectName(refName, topic, isKey, schema)
        }
        return refDelegate.subjectName(refName, topic, isKey, schema)
    }
}
