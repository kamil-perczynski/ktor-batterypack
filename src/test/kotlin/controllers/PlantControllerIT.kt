package io.github.kperczynski.controllers

import io.github.kperczynski.domain.plant.PlantRepo
import io.github.kperczynski.infra.KtorBatteriesIT
import io.github.kperczynski.domain.somePlant
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.koin.ktor.plugin.koin
import java.util.UUID

class PlantControllerIT : KtorBatteriesIT() {

    private var plantRepo: PlantRepo = application.koin().get<PlantRepo>()

    @Test
    fun `should list plants`() = runTest {
        plantRepo.create(
            somePlant().copy(
                id = 0u,
                externalId = UUID.randomUUID(),
                tempIdentityId = UUID.randomUUID()
            )
        )

        val response = httpClient.get("/api/plants")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `should get a plant`() = runTest {
        val createdPlant = plantRepo.create(
            somePlant().copy(
                id = 0u,
                externalId = UUID.randomUUID(),
                tempIdentityId = UUID.randomUUID()
            )
        )

        val response = httpClient.get("/api/plants/${createdPlant.id}")

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `should return 404 for missing plant`() = runTest {
        val response = httpClient.get("/api/plants/99999")

        assertThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

}
