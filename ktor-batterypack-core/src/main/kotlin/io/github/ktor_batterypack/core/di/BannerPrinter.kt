package io.github.ktor_batterypack.core.di

import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BannerPrinter::class.java)

/**
 * Prints an optional banner text to the log during initialization.
 */
@Singleton
class BannerPrinter(val bannerText: String?) : BootstrapCallback {

    /**
     * Logs the banner text if it is not null.
     */
    override fun onBootstrap() {
        if (bannerText != null) {
            log.info("\n{}", bannerText.prependIndent(" "))
        }
    }

}
