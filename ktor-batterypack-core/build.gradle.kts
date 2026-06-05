import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.koin.compiler)
    `maven-publish`
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
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.serialization.jackson3)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.hoplite.core)
    implementation(libs.hoplite.yaml)

    implementation(libs.koin.annotations)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    implementation(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
    testImplementation(ktorLibs.server.testHost)
}
