import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.kperczynski.MainKt"
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":ktor-batterypack-core"))

    implementation(ktorLibs.serialization.jackson3)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(ktorLibs.server.statusPages)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(ktorLibs.client.core)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.client.logging)

    implementation(libs.exposed.core)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.r2dbc)
    implementation(libs.hikari)
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)
    implementation(libs.conform)
    implementation(libs.koin.annotations)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)
    implementation(libs.lettuce.core)
    implementation(libs.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
    testImplementation(libs.testcontainers)
}

tasks.register<Sync>("dockerDist") {
    dependsOn("jar")
    group = "distribution"
    description = "Creates Docker-friendly distribution with separated app and dependency layers"

    into(layout.buildDirectory.dir("docker-dist"))

    into("lib") {
        from(configurations.runtimeClasspath)
    }

    into("app") {
        from(tasks.jar)
    }
}

tasks.assemble {
    dependsOn("dockerDist")
}
