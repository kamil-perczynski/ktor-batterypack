package io.github.kperczynski

import com.sksamuel.hoplite.indent
import io.github.kperczynski.config.AppConfig
import io.github.kperczynski.di.InitCallback
import org.koin.core.annotation.Singleton
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BannerPrinter::class.java)

@Singleton
class BannerPrinter(private val appConfig: AppConfig) : InitCallback {

    override fun onInit() {
        if (appConfig.banner != null) {
            log.info("\n{}", appConfig.banner.indent("  "))
        }
    }

}
