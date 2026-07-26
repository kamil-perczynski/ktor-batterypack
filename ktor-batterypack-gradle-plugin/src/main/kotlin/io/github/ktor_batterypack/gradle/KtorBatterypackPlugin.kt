package io.github.ktor_batterypack.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

private const val TASKS_GROUP = "ktor-batterypack"

/**
 * Gradle plugin that augments Ktor projects with Docker-friendly distribution tasks.
 *
 * Applying this plugin registers the following tasks:
 *
 * | Task | Group | Description |
 * |------|-------|-------------|
 * | `dockerDist` | distribution | Copies the application JAR and runtime dependencies into `build/docker-dist` with separate `app` and `lib` layers. |
 * | `bootstrapDockerfile` | distribution | Generates a `Dockerfile` that consumes the `dockerDist` output. |
 * | `bootstrapDockerignore` | distribution | Generates a `.dockerignore` that keeps the Docker context minimal. |
 *
 * The `assemble` task is automatically configured to depend on `dockerDist`, so a
 * regular `./gradlew build` produces the Docker distribution without any extra
 * step.
 *
 * The plugin exposes a `ktorBatterypack { }` extension block (see
 * [KtorBatterypackExtension]) for optional customisation, such as overriding
 * the detected application main class.
 */
class KtorBatterypackPlugin : Plugin<Project> {
    companion object {
        private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"
    }

    /**
     * Configures the given [project] by registering tasks and wiring defaults.
     *
     * @param project the Gradle project to which the plugin is applied
     */
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("ktorBatterypack", KtorBatterypackExtension::class.java)

        project.plugins.apply(KSP_PLUGIN_ID)

        val pluginJar =
            project.files(File(KtorBatterypackPlugin::class.java.protectionDomain.codeSource.location.toURI()))

        val jacksonDatabind =
            project.dependencies.create("tools.jackson.core:jackson-databind:3.1.3")
        val jacksonDataformatYaml =
            project.dependencies.create("tools.jackson.dataformat:jackson-dataformat-yaml:3.1.3")

        project.dependencies.add("ksp", pluginJar)
        project.dependencies.add("ksp", jacksonDatabind)
        project.dependencies.add("ksp", jacksonDataformatYaml)

        project.afterEvaluate {
            val className = extension.configMetadataClass ?: return@afterEvaluate
            project.extensions.configure(com.google.devtools.ksp.gradle.KspExtension::class.java) { ksp ->
                ksp.arg("configMetadataClass", className)
            }

            project.extensions.configure(KotlinJvmProjectExtension::class.java) { kotlin ->
                kotlin.sourceSets.getByName("main").resources.srcDir(
                    project.layout.buildDirectory.dir("generated/ksp/main/resources")
                )
            }

            project.tasks.named("processResources", ProcessResources::class.java) { task ->
                task.dependsOn("kspKotlin")
                task.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
            }
        }

        val dockerDist = project.tasks.register("dockerDist", Sync::class.java) { task ->
            task.dependsOn("jar")
            task.group = TASKS_GROUP
            task.description =
                "Creates Docker-friendly distribution with separated app and dependency layers"

            task.into(project.layout.buildDirectory.dir("docker-dist"))

            task.into("lib") {
                it.from(project.configurations.getByName("runtimeClasspath"))
            }

            task.into("app") {
                it.from(project.tasks.getByName("jar"))
            }
        }

        val mainClassProvider: Provider<String> = project.tasks.named("jar", Jar::class.java)
            .map { jarTask ->
                extension.mainClass ?: jarTask.manifest.attributes["Main-Class"] as? String
            }
            .orElse("io.ktor.server.netty.EngineMain")

        project.tasks.register("bootstrapDockerfile", BootstrapDockerfileTask::class.java) { task ->
            task.group = TASKS_GROUP
            task.description = "Bootstraps a Dockerfile for the dockerDist layout"
            task.outputFile.set(project.layout.projectDirectory.file("Dockerfile"))
            task.mainClass.set(mainClassProvider)
        }

        project.tasks.register(
            "bootstrapDockerignore",
            BootstrapDockerignoreTask::class.java
        ) { task ->
            task.group = TASKS_GROUP
            task.description = "Bootstraps a .dockerignore for the dockerDist layout"
            task.outputFile.set(project.layout.projectDirectory.file(".dockerignore"))
        }

        project.tasks.named("assemble").configure { task ->
            task.dependsOn(dockerDist)
        }
    }
}
