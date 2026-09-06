package io.github.kperczynski.infra.persistence

import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/** Shared [JsonMapper] used to serialize JSONB columns in the persistence layer. */
val exposedJsonMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()
