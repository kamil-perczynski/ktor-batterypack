plugins {
    kotlin("jvm") version "2.3.20"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.kperczynski"
version = "0.0.2-alpha"

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
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kamil-perczynski/ktor-batterypack")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GH_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GH_TOKEN")
            }
        }
    }
}
