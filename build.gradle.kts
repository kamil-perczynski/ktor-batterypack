plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.kperczynski"
version = "0.0.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}
