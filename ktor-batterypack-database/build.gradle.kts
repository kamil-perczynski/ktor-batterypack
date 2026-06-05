import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.koin.compiler)
    `java-library`
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

group = "io.github.kperczynski"
version = "1.0.0-SNAPSHOT"

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":ktor-batterypack-core"))
    api(project(":ktor-batterypack-metrics"))

    api(libs.hikari)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)

    implementation(libs.hoplite.core)

    implementation(libs.koin.annotations)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
