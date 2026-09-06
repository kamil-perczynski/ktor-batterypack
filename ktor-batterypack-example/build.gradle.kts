import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.koin.compiler)
    id("org.openapi.generator") version "7.25.0"
    id("ktor-batterypack-gradle-plugin")
}

ktorBatterypack {
    configMetadataClass = "io.github.kperczynski.infra.AppProps"
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

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.kperczynski.MainKt"
    }
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    ksp(project(":ktor-batterypack-validation-ksp"))
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.ws.rs.api)
    implementation(libs.jakarta.annotation.api)

    implementation(project(":ktor-batterypack-annotations"))
    implementation(project(":ktor-batterypack-validation"))
    implementation(project(":ktor-batterypack-core"))
    implementation(project(":ktor-batterypack-database"))
    implementation(project(":ktor-batterypack-metrics"))
    implementation(project(":ktor-batterypack-redis"))

    implementation(libs.exposed.core)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.hikari)

    implementation(libs.postgresql)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(kotlin("test"))
    testImplementation(project(":ktor-batterypack-database-testing"))
    testImplementation(project(":ktor-batterypack-redis-testing"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(ktorLibs.client.mock)
    testImplementation(libs.testcontainers)
}

/**
 * https://openapi-generator.tech/docs/generators/kotlin-spring/
 */
openApiGenerate {
    generatorName.set("kotlin-server")
    library.set("jaxrs-spec")
    generateApiDocumentation.set(false)
    inputSpec.set("$projectDir/src/main/resources/static/schema/api.yaml")
    outputDir.set("$projectDir/build/generated")
    apiPackage.set("pl.kperczynski.florin.rest")
    modelPackage.set("pl.kperczynski.florin.rest.dto")
    modelNameSuffix.set("Dto")
    auth.set("false")

    templateDir.set("$projectDir/src/main/resources/templateDir")

    typeMappings.set(
        mapOf(
            "double" to "java.math.BigDecimal",
        )
    )

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "allowUnicodeIdentifiers" to "true",
            "delegatePattern" to "true",
            "useTags" to "true",
            "useJakartaEe" to "true",
            "serializationLibrary" to "jackson",
            "useJackson3" to "true",
            "omitGradleWrapper" to "true",
            "enumPropertyNaming" to "original",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "useCoroutines" to "true",
        )
    )
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

tasks.whenTaskAdded {
    if (name == "kspKotlin") {
        dependsOn(tasks.openApiGenerate)
    }
}

ksp {
    arg(
        "ktor.validation.constraint.dir",
        project.layout.projectDirectory.dir("src/main/resources/constraints").toString()
    )
}

sourceSets {
    main { kotlin { srcDir("build/generated/src/main/kotlin") } }
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}