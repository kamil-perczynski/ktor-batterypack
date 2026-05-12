plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
    application
}

tasks.test {
    useJUnitPlatform()
}

group = "io.github.kperczynski"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.github.kperczynski.MainKt"
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(ktorLibs.serialization.jackson)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation("io.ktor:ktor-server-metrics-micrometer:3.4.0")
    implementation(ktorLibs.server.statusPages)

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")

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
    implementation(libs.koin.annotations)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
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
