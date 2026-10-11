import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
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
    api(project(":ktor-batterypack-kafka"))

    api(libs.kafka.clients)
    api(libs.testcontainers)
    api(libs.testcontainers.kafka)

    compileOnly(libs.confluent.schema.registry.client)
    compileOnly(libs.confluent.avro.serializer)

    implementation(libs.koin.annotations)
    implementation(libs.koin.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.koin.ktor)
    testImplementation(libs.logback.classic)
}
