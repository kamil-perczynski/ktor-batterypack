package io.github.kperczynski.controllers

import io.github.kperczynski.controllers.PlantsApiValidator.Companion.plantsApiValidator
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.JsonBinder
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton
import pl.kperczynski.florin.rest.PlantsApi
import pl.kperczynski.florin.rest.dto.AckDto
import pl.kperczynski.florin.rest.dto.CareScheduleDto
import pl.kperczynski.florin.rest.dto.ListPlantsParamsParameterDto
import pl.kperczynski.florin.rest.dto.PlantCreateDto
import pl.kperczynski.florin.rest.dto.PlantDto
import pl.kperczynski.florin.rest.dto.PlantListingDto
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

@Singleton
class PlantsController(
    private val binder: JsonBinder,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController, PlantsApi {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            post("/api/plants") {
                permissionChecker.checkPermission(call.principal, PLANT_CREATE)
                val query = binder.bindQueryParams<ListPlantsParamsParameterDto>(
                    call,
                    plantsApiValidator::checkListPlantsParamsParameterDto
                )

                val body =
                    binder.bindBody<PlantCreateDto>(call, plantsApiValidator::checkPlantCreateDto)

                val response = listPlants(query)

                call.respond(HttpStatusCode.OK, mapOf("body" to body, "query" to query))
            }
        }
    }

    override suspend fun claimPlant(plantCreateDto: PlantCreateDto): PlantDto {
        TODO("Not yet implemented")
    }

    override suspend fun deletePlant(plantId: UUID): AckDto {
        TODO("Not yet implemented")
    }

    override suspend fun fetchCareSchedule(
        plantId: UUID?,
        dueDateGte: LocalDate?,
        dueDateLte: LocalDate?
    ): CareScheduleDto {
        TODO("Not yet implemented")
    }

    override suspend fun fetchPlantImage(imageId: UUID, filename: String) {
        TODO("Not yet implemented")
    }

    override suspend fun identifyPlant(
        image1InputStream: InputStream?,
        image2InputStream: InputStream?,
        image3InputStream: InputStream?
    ): PlantDto {
        TODO("Not yet implemented")
    }

    override suspend fun listPlants(params: ListPlantsParamsParameterDto?): PlantListingDto {
        return PlantListingDto(total = 10, emptyList())
    }

}