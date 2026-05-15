package io.github.kperczynski.infra.monitoring

import io.micrometer.core.instrument.MeterRegistry

interface MeterRegistryInit {
    fun init(meterRegistry: MeterRegistry)
}