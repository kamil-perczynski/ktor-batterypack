plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    id("ktor-batterypack-publishing")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktor-batterypack-validation"))
    implementation(project(":ktor-batterypack-annotations"))
    implementation(libs.jakarta.validation.api)
    compileOnly(libs.ksp.symbol.processing.api)
    implementation(libs.guava)
    implementation(libs.handlebars)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)
    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
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