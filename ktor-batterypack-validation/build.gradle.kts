plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("ktor-batterypack-publishing")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
}