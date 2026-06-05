package io.github.ktor_batterypack.metrics

import io.micrometer.core.instrument.MeterRegistry

interface MeterRegistryInit {
    fun init(meterRegistry: MeterRegistry)
}
