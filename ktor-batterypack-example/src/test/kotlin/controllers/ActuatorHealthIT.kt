package io.github.kperczynski.controllers

import io.github.kperczynski.infra.KtorBatteriesIT
import io.github.ktor_batterypack.core.health.HealthStatus
import io.github.ktor_batterypack.core.health.LivenessResponse
import io.github.ktor_batterypack.core.health.ReadinessResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActuatorHealthIT : KtorBatteriesIT() {

    @Test
    fun `should return liveness up`() = runTest {
        val response = httpClient.get("/actuator/health/liveness")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val body = response.body<LivenessResponse>()
        assertThat(body.liveness).isEqualTo(HealthStatus.UP)
    }

    @Test
    fun `should return readiness up with all checks`() = runTest {
        val response = httpClient.get("/actuator/health/readiness")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)

        val body = response.body<ReadinessResponse>()
        assertThat(body.status).isEqualTo(HealthStatus.UP)
        assertThat(body.checks).containsEntry("database", HealthStatus.UP)
        assertThat(body.checks).containsEntry("diskSpace", HealthStatus.UP)
        assertThat(body.checks).containsEntry("redis", HealthStatus.UP)
    }

    @Test
    fun `should expose prometheus metrics`() = runTest {
        val response = httpClient.get("/actuator/prometheus")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.contentType()).isEqualTo(ContentType.Text.Plain.withCharset(Charsets.UTF_8))

        val body = response.body<String>()
        assertThat(body).contains("jvm_")
        assertThat(body).contains("ktor_http_server_requests")
    }

}
