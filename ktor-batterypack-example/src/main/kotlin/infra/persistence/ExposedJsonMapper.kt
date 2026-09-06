package io.github.kperczynski.infra.persistence

import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

val exposedJsonMapper: JsonMapper = JsonMapper.builder()
    .addModule(KotlinModule.Builder().build())
    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    .build()
