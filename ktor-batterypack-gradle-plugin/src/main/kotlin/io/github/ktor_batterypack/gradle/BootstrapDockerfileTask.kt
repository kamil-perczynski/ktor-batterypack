package io.github.ktor_batterypack.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generates a production-ready `Dockerfile` for projects that use the
 * `dockerDist` layout (separated app and dependency layers).
 *
 * The generated Dockerfile is based on `eclipse-temurin:25-jdk-alpine` and
 * copies the output of the `dockerDist` task (`build/docker-dist/lib` and
 * `build/docker-dist/app`) into the image.
 *
 * The main class written into the `CMD` instruction is controlled by the
 * [mainClass] input property. Consumers should wire it to the value
 * resolved from [KtorBatterypackExtension.mainClass] or the `jar` manifest.
 *
 * @see KtorBatterypackPlugin
 */
abstract class BootstrapDockerfileTask : DefaultTask() {

    /**
     * Fully-qualified name of the application's entry point class.
     *
     * This value is injected into the `CMD` instruction of the generated
     * Dockerfile. It is expected to be provided at task configuration time.
     */
    @get:Input
    abstract val mainClass: Property<String>

    /**
     * Destination file where the `Dockerfile` will be written.
     *
     * Defaults to `Dockerfile` in the project root directory.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Writes the Dockerfile content to [outputFile].
     *
     * The produced Dockerfile:
     * - uses `eclipse-temurin:25-jdk-alpine` as the base image
     * - sets `/app` as the working directory
     * - copies `build/docker-dist/lib` and `build/docker-dist/app`
     * - exposes port `8080`
     * - configures `JAVA_TOOL_OPTIONS` for containerised JVM tuning
     * - starts the application with the supplied [mainClass]
     */
    @TaskAction
    fun bootstrap() {
        val content = buildString {
            appendLine("FROM eclipse-temurin:25-jdk-alpine")
            appendLine("WORKDIR /app")
            appendLine()
            appendLine("COPY build/docker-dist/lib lib")
            appendLine("COPY build/docker-dist/app .")
            appendLine()
            appendLine("EXPOSE 8080")
            appendLine()
            appendLine("ENV JAVA_TOOL_OPTIONS=\"--enable-native-access=ALL-UNNAMED -XX:ActiveProcessorCount=4 -XX:MaxRAMPercentage=80 -XX:+UseCompactObjectHeaders\"")
            appendLine("CMD [\"java\", \"-cp\", \"*:lib/*\", \"${mainClass.get()}\"]")
        }
        val file = outputFile.get().asFile
        file.writeText(content)
        logger.lifecycle("Bootstrapped ${file.absolutePath}")
    }
}
