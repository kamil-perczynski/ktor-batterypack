plugins {
    kotlin("jvm") version "2.3.20"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.kperczynski"
version = "0.0.7-alpha"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(25)
}

java {
    withJavadocJar()
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("ktorBatterypack") {
            id = "ktor-batterypack-gradle-plugin"
            implementationClass = "io.github.ktor_batterypack.gradle.KtorBatterypackPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kamil-perczynski/ktor-batterypack")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
