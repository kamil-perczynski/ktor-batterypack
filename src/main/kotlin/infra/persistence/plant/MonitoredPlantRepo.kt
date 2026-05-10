package io.github.kperczynski.infra.persistence.plant

import io.github.kperczynski.domain.plant.PlantRepo
import io.github.kperczynski.domain.plant.model.Plant
import io.github.kperczynski.infra.monitoring.MonitoredTransactions
import org.koin.core.annotation.Singleton
import java.util.UUID

@Singleton(binds = [PlantRepo::class])
class MonitoredPlantRepo(
    private val delegate: ExposedPlantRepo,
    private val monitoredTransactions: MonitoredTransactions
) : PlantRepo {

    override fun create(plant: Plant): Plant {
        return monitoredTransactions.tx("PlantRepo.create") {
            delegate.create(plant)
        }
    }

    override fun find(id: UInt): Plant {
        return monitoredTransactions.tx("PlantRepo.find") {
            delegate.find(id)
        }
    }

    override fun findAll(): List<Plant> {
        return monitoredTransactions.tx("PlantRepo.findAll") {
            delegate.findAll()
        }
    }

    override fun update(plant: Plant) {
        monitoredTransactions.tx("PlantRepo.update") {
            delegate.update(plant)
        }
    }

    override fun delete(id: UInt) {
        monitoredTransactions.tx("PlantRepo.delete") {
            delegate.delete(id)
        }
    }

    override fun findByTempIdentityId(tempIdentityId: UUID): Plant? {
        return monitoredTransactions.tx("PlantRepo.findByTempIdentityId") {
            delegate.findByTempIdentityId(tempIdentityId)
        }
    }
}
