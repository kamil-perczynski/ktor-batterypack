rootProject.name = "ktor-frame"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("ktor-batterypack-gradle-plugin")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.4.0")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("ktor-batterypack-core")
include("ktor-batterypack-database")
include("ktor-batterypack-metrics")
include("ktor-batterypack-redis")
include("ktor-batterypack-redis-testing")
include("ktor-batterypack-database-testing")