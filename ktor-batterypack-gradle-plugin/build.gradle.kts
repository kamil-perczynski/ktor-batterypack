plugins {
    kotlin("jvm") version "2.3.20"
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(25)
}

gradlePlugin {
    plugins {
        create("ktorBatterypack") {
            id = "ktor-batterypack"
            implementationClass = "io.github.ktor_batterypack.gradle.KtorBatterypackPlugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
