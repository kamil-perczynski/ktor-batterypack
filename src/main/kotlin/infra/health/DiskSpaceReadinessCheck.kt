package io.github.kperczynski.infra.health

import io.github.kperczynski.libs.health.HealthCheckResult
import io.github.kperczynski.libs.health.HealthStatus
import io.github.kperczynski.libs.health.ReadinessCheck
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

private val log = LoggerFactory.getLogger(DiskSpaceReadinessCheck::class.java)

class DiskSpaceReadinessCheck(
    private val path: Path = FileSystems.getDefault().getPath(".").toAbsolutePath().normalize(),
    private val thresholdBytes: Long = 1024 * 1024 * 100 // 100 MB default threshold
) : ReadinessCheck {

    override fun check(): HealthCheckResult {
        val status = try {
            val fileStore = Files.getFileStore(path)
            val usableSpace = fileStore.usableSpace
            if (usableSpace > thresholdBytes) {
                HealthStatus.UP
            } else {
                log.warn("Disk space check failed. Usable space: {} bytes, threshold: {} bytes", usableSpace, thresholdBytes)
                HealthStatus.DOWN
            }
        } catch (e: Exception) {
            log.warn("Disk space check failed with exception", e)
            HealthStatus.DOWN
        }

        return HealthCheckResult(
            name = "diskSpace",
            status = status
        )
    }
}
