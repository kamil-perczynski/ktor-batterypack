package io.github.kperczynski.infra.actuator

import io.github.kperczynski.infra.persistence.DatabaseReadinessCheck
import io.github.ktor_batterypack.core.health.ReadinessCheck
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import javax.sql.DataSource

@Module
@Configuration
class ActuatorModule {

    @Singleton(binds = [ReadinessCheck::class])
    fun databaseCheck(dataSource: DataSource): DatabaseReadinessCheck {
        return DatabaseReadinessCheck(dataSource)
    }

}
