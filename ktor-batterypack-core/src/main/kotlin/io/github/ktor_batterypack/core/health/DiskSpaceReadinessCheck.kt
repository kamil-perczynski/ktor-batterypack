package io.github.ktor_batterypack.core.health

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

private val log = LoggerFactory.getLogger(DiskSpaceReadinessCheck::class.java)

@Suppress("ObjectPropertyName")
private const val _100MB = 1024 * 1024 * 100L

/** Readiness check that verifies the available disk space at a given path exceeds a threshold. */
class DiskSpaceReadinessCheck(
    private val path: Path = FileSystems.getDefault().getPath(".").toAbsolutePath().normalize(),
    private val thresholdBytes: Long = _100MB
) : ReadinessCheck {

    /** Checks the available disk space at the configured path. */
    override suspend fun check(): HealthCheckResult {
        val status = try {
            val fileStore = withContext(Dispatchers.IO) {
                Files.getFileStore(path)
            }
            val usableSpace = fileStore.usableSpace

            if (usableSpace > thresholdBytes) {
                HealthStatus.UP
            } else {
                log.warn(
                    "Disk space check failed. Usable space: {} bytes, threshold: {} bytes",
                    usableSpace,
                    thresholdBytes
                )
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
