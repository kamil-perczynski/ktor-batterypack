import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.koin.compiler)
    `java-library`
    id("ktor-batterypack-publishing")
}

// Required because Java 24+ (JEP 472) restricts System::load/loadLibrary.
// Netty loads native libraries from an unnamed module, which triggers warnings
// (and will eventually be blocked) without this flag.
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.test {
    useJUnitPlatform()
    // Same JEP 472 workaround for test JVMs (Netty native library loading).
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

kotlin {
    jvmToolchain(25)
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    api(project(":ktor-batterypack-redis"))

    api(libs.testcontainers)

    implementation(libs.koin.annotations)
    implementation(libs.koin.core)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.koin.ktor)
    testImplementation(libs.logback.classic)
}
