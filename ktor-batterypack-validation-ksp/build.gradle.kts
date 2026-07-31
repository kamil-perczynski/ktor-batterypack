plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktor-batterypack-annotations"))

    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10")
    implementation(libs.handlebars)
    testImplementation(kotlin("test"))
    testImplementation(libs.assertj.core)
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}