package io.github.ktor_batterypack.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Generates a `.dockerignore` file tuned for the `dockerDist` workflow.
 *
 * The generated file ignores **all** files by default and then selectively
 * un-ignores only the build outputs that are actually needed inside a
 * Docker image:
 * - `build/distributions` (optional fallback)
 * - `build/docker-dist` (the primary Docker distribution layout)
 *
 * This keeps the Docker build context small and avoids leaking source code
 * or local configuration into the image.
 *
 * @see KtorBatterypackPlugin
 */
abstract class BootstrapDockerignoreTask : DefaultTask() {

    /**
     * Destination file where the `.dockerignore` will be written.
     *
     * Defaults to `.dockerignore` in the project root directory.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Writes the `.dockerignore` content to [outputFile].
     *
     * The produced file contains three lines:
     * 1. `*` — ignore everything
     * 2. `!build/distributions` — except distribution archives
     * 3. `!build/docker-dist` — except the docker-friendly dist directory
     */
    @TaskAction
    fun bootstrap() {
        val content = buildString {
            appendLine("*")
            appendLine("!build/distributions")
            appendLine("!build/docker-dist")
        }
        val file = outputFile.get().asFile
        file.writeText(content)
        logger.lifecycle("Bootstrapped ${file.absolutePath}")
    }
}
