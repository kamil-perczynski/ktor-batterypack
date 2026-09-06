plugins {
    kotlin("jvm") version "2.3.20"
    `java-gradle-plugin`
    `maven-publish`
}

group = "io.github.kperczynski"
version = "0.0.10-alpha"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(25)
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.10")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    implementation("tools.jackson.core:jackson-databind:3.1.3")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.3")
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation(kotlin("test"))
    testImplementation("dev.zacsweers.kctfork:core:0.13.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    testImplementation("dev.zacsweers.kctfork:ksp:0.13.0") {
        exclude(group = "com.google.devtools.ksp")
    }
    testRuntimeOnly("com.google.devtools.ksp:symbol-processing:2.3.10")
    testRuntimeOnly("com.google.devtools.ksp:symbol-processing-common-deps:2.3.10")
    testRuntimeOnly("com.google.devtools.ksp:symbol-processing-aa-embeddable:2.3.10")
}

gradlePlugin {
    plugins {
        create("ktorBatterypack") {
            id = "ktor-batterypack-gradle-plugin"
            implementationClass = "io.github.ktor_batterypack.gradle.KtorBatterypackPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kamil-perczynski/ktor-batterypack")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
