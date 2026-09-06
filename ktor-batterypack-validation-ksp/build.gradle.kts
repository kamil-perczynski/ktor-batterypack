plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktor-batterypack-validation"))
    implementation(project(":ktor-batterypack-annotations"))
    implementation(libs.jakarta.validation.api)
    compileOnly("com.google.devtools.ksp:symbol-processing-api:2.3.10")
    implementation("com.google.guava:guava:33.6.0-jre")
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

tasks.test {
    useJUnitPlatform()
}