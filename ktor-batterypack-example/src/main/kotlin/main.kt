package io.github.kperczynski

import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.netty.EngineMain

fun main(args: Array<String>) {
    dotenv {
        systemProperties = true
        ignoreIfMissing = true
    }

    EngineMain.main(args)
}
