package io.github.ktor_batterypack.gradle

/**
 * Extension block for the `ktorBatterypack` DSL.
 *
 * Allows users to customise plugin behaviour, such as overriding
 * the detected application main class.
 *
 * Example usage in `build.gradle.kts`:
 * ```kotlin
 * ktorBatterypack {
 *     mainClass = "com.example.MyMainKt"
 * }
 * ```
 */
open class KtorBatterypackExtension {

    /**
     * Fully-qualified name of the application's entry point.
     *
     * When left `null`, the plugin falls back to the `Main-Class` attribute
     * declared in the `jar` task manifest. If that is also absent, the
     * default Ktor Netty engine main (`io.ktor.server.netty.EngineMain`)
     * is used.
     */
    var mainClass: String? = null
}
