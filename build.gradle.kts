plugins {
    alias(libs.plugins.kotlin.jvm)
    `version-catalog`
    id("ktor-batterypack-publishing")
}

group = "io.github.ktor_batterypack"
version = libs.versions.ktor.batterypack.get()

allprojects {
    group = rootProject.group
    version = rootProject.version
}

catalog {
    versionCatalog {
        from(files("gradle/libs.versions.toml"))
    }
}

ktorBatterypackPublishing {
    component = "versionCatalog"
    artifactId = "ktor-batterypack-versions-catalog"
}