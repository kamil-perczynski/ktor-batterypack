package io.github.kperczynski.infra.actuator

import io.github.kperczynski.libs.health.HealthController
import io.github.kperczynski.libs.health.ReadinessCheck
import io.github.kperczynski.libs.health.ReadinessEndpoint
import io.github.kperczynski.libs.health.checks.DatabaseReadinessCheck
import io.github.kperczynski.libs.health.checks.DiskSpaceReadinessCheck
import io.github.kperczynski.libs.ktor.KtorController
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import javax.sql.DataSource

@Module
@Configuration
class ActuatorModule {

    @Singleton(binds = [ReadinessCheck::class])
    fun diskSpaceCheck(): DiskSpaceReadinessCheck {
        return DiskSpaceReadinessCheck()
    }

    @Singleton(binds = [ReadinessCheck::class])
    fun databaseCheck(dataSource: DataSource): DatabaseReadinessCheck {
        return DatabaseReadinessCheck(dataSource)
    }

    @Singleton
    fun readinessEndpoint(checks: List<ReadinessCheck>): ReadinessEndpoint {
        return ReadinessEndpoint(checks)
    }

    @Singleton(binds = [KtorController::class])
    fun healthController(readinessEndpoint: ReadinessEndpoint): HealthController {
        return HealthController(readinessEndpoint)
    }

}