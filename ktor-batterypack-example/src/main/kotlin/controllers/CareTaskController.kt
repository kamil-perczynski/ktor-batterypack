package io.github.kperczynski.controllers

import io.github.kperczynski.controllers.PlantsApiValidator.Companion.plantsApiValidator
import io.github.kperczynski.infra.oidc.OidcAuthScheme
import io.github.ktor_batterypack.authz.PermissionChecker
import io.github.ktor_batterypack.core.ktor.JsonBinder
import io.github.ktor_batterypack.core.ktor.KtorController
import io.ktor.server.auth.authenticateWith
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.koin.core.annotation.Singleton
import pl.kperczynski.florin.rest.CareTasksApi
import pl.kperczynski.florin.rest.dto.CareTaskDto
import pl.kperczynski.florin.rest.dto.CareTaskSnoozeRequestDto
import pl.kperczynski.florin.rest.dto.CareTaskStatusDto
import pl.kperczynski.florin.rest.dto.CareTaskStatusUpdateDto
import pl.kperczynski.florin.rest.dto.CareTaskTypeDto
import java.time.LocalDate
import java.util.UUID

@Singleton
class CareTaskController(
    private val binder: JsonBinder,
    private val authScheme: OidcAuthScheme,
    private val permissionChecker: PermissionChecker,
) : KtorController, CareTasksApi {

    @OptIn(ExperimentalKtorApi::class)
    override fun register(routing: Routing) {
        routing.authenticateWith(authScheme.jwt) {
            post("/api/care-tasks/{taskId}/snooze") {
                permissionChecker.checkPermission(call.principal, CARE_TASK_SNOOZE)
                val dto = binder.bindBody<CareTaskSnoozeRequestDto>(
                    call,
                    plantsApiValidator::validateCareTaskSnoozeRequestDto
                )

                val taskId = call.pathParameters.getOrFail("taskId").let { UUID.fromString(it) }

                val taskDto = snoozeCareTask(taskId, dto)
                call.respond(taskDto)
            }

            patch("/api/care-tasks/{taskId}/status") {
                permissionChecker.checkPermission(call.principal, CARE_TASK_UPDATE)
                val dto = binder.bindBody<CareTaskStatusUpdateDto>(
                    call,
                    plantsApiValidator::validateCareTaskStatusUpdateDto
                )

                val taskId = call.pathParameters.getOrFail("taskId").let { UUID.fromString(it) }

                val taskDto = updateCareTaskStatus(taskId, dto)
                call.respond(taskDto)
            }
        }
    }

    override suspend fun completeAllTasksForDay(date: LocalDate): List<CareTaskDto> {
        TODO("Not yet implemented")
    }

    override suspend fun snoozeCareTask(
        taskId: UUID,
        careTaskSnoozeRequestDto: CareTaskSnoozeRequestDto
    ): CareTaskDto {
        return CareTaskDto(
            id = UUID.randomUUID(),
            type = CareTaskTypeDto.WATERING,
            plantId = UUID.randomUUID(),
            dueDate = LocalDate.now(),
            status = CareTaskStatusDto.TODO
        )
    }

    override suspend fun updateCareTaskStatus(
        taskId: UUID,
        careTaskStatusUpdateDto: CareTaskStatusUpdateDto
    ): CareTaskDto {
        return CareTaskDto(
            id = UUID.randomUUID(),
            plantId = UUID.randomUUID(),
            dueDate = LocalDate.now().plusDays(14),
            status = CareTaskStatusDto.TODO,
            type = CareTaskTypeDto.FERTILIZING,
        )
    }

}