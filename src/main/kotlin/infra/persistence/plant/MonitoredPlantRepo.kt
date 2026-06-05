package io.github.kperczynski.infra.persistence.plant

import io.github.kperczynski.domain.plant.PlantRepo
import io.github.kperczynski.domain.plant.model.Plant
import io.github.ktor_batterypack.database.MonitoredTransactions
import org.koin.core.annotation.Singleton
import java.util.UUID

@Singleton(binds = [PlantRepo::class])
class MonitoredPlantRepo(
    private val delegate: ExposedPlantRepo,
    private val monitoredTransactions: MonitoredTransactions
) : PlantRepo {

    override suspend fun create(plant: Plant): Plant {
        return monitoredTransactions.suspendTx("PlantRepo.create") {
            delegate.create(plant)
        }
    }

    override suspend fun find(id: UInt): Plant {
        return monitoredTransactions.suspendTx("PlantRepo.find") {
            delegate.find(id)
        }
    }

    override suspend fun findAll(): List<Plant> {
        return monitoredTransactions.suspendTx("PlantRepo.findAll") {
            delegate.findAll()
        }
    }

    override suspend fun update(plant: Plant) {
        monitoredTransactions.suspendTx("PlantRepo.update") {
            delegate.update(plant)
        }
    }

    override suspend fun delete(id: UInt) {
        monitoredTransactions.suspendTx("PlantRepo.delete") {
            delegate.delete(id)
        }
    }

    override suspend fun findByTempIdentityId(tempIdentityId: UUID): Plant? {
        return monitoredTransactions.suspendTx("PlantRepo.findByTempIdentityId") {
            delegate.findByTempIdentityId(tempIdentityId)
        }
    }
}
