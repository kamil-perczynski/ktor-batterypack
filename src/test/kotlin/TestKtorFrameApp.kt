package io.github.kperczynski

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@KoinApplication
object TestKtorFrameApp

@Module
@Configuration
@ComponentScan("io.github.kperczynski")
class TestKtorFrameAppModule