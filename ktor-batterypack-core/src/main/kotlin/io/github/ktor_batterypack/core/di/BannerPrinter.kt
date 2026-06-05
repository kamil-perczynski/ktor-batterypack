package io.github.ktor_batterypack.core.di

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BannerPrinter::class.java)

/**
 * Prints an optional banner text to the log during initialization.
 */
class BannerPrinter(val bannerText: String?) : InitCallback {

    /**
     * Logs the banner text if it is not null.
     */
    override fun onInit() {
        if (bannerText != null) {
            log.info("\n{}", bannerText.prependIndent(" "))
        }
    }

}
