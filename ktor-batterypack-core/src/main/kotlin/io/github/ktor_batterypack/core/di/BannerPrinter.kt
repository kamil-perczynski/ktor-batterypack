package io.github.ktor_batterypack.core.di

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BannerPrinter::class.java)

class BannerPrinter(val bannerText: String?) : InitCallback {

    override fun onInit() {
        if (bannerText != null) {
            log.info("\n{}", bannerText.prependIndent(" "))
        }
    }

}
