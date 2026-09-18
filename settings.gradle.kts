rootProject.name = "ktor-batterypack"

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
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.2")
    }
}

include("ktor-batterypack-annotations")
include("ktor-batterypack-core")
include("ktor-batterypack-database")
include("ktor-batterypack-metrics")
include("ktor-batterypack-redis")
include("ktor-batterypack-redis-testing")
include("ktor-batterypack-database-testing")
include("ktor-batterypack-example")
include("ktor-batterypack-validation-ksp")
include("ktor-batterypack-validation")
