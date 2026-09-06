import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.koin.compiler)
    `maven-publish`
}

// Required because Java 24+ (JEP 472) restricts System::load/loadLibrary.
// `Netty` loads native libraries from an unnamed module, which triggers warnings
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
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

dependencies {
    api(ktorLibs.server.core)
    api(ktorLibs.server.netty)
    api(ktorLibs.server.contentNegotiation)
    api(ktorLibs.server.statusPages)
    api(ktorLibs.serialization.jackson3)

    api(libs.jackson.databind)
    api(libs.jackson.module.kotlin)

    api(libs.hoplite.core)
    api(libs.hoplite.yaml)

    api(libs.koin.annotations)
    api(libs.koin.core)
    api(libs.koin.ktor)

    api(ktorLibs.server.config.yaml)

    api(ktorLibs.client.core)
    api(ktorLibs.client.cio)
    api(ktorLibs.client.contentNegotiation)
    api(ktorLibs.client.logging)

    api(libs.logback.classic)

    implementation(libs.jakarta.validation.api)
    implementation(project(":ktor-batterypack-validation"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
    testImplementation(ktorLibs.server.testHost)
}
