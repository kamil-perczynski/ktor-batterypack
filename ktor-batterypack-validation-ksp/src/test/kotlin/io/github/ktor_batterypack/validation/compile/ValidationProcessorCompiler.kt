package io.github.ktor_batterypack.validation.compile

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspProcessorOptions
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import io.github.ktor_batterypack.validation.ConstraintError
import io.github.ktor_batterypack.validation.InvalidResult
import io.github.ktor_batterypack.validation.ValidationResult
import io.github.ktor_batterypack.validation.ksp.ValidationGeneratorProvider
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.io.OutputStream

class ValidationProcessorCompiler {

    private val jsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .build()

    fun compile(sources: List<SourceFile>, processorOptions: Map<String, String> = emptyMap()): JvmCompilationResult {
        val compilation = KotlinCompilation()
        compilation.sources = sources
        compilation.inheritClassPath = true
        compilation.messageOutputStream = OutputStream.nullOutputStream()
        compilation.useKsp2()
        compilation.symbolProcessorProviders = mutableListOf(ValidationGeneratorProvider())
        compilation.kspProcessorOptions = processorOptions.toMutableMap()
        return compilation.compile()
    }

    fun generatedSource(result: JvmCompilationResult, fileName: String): String {
        return result.sourcesGeneratedBySymbolProcessor
            .first { file -> file.name == fileName }
            .readText()
    }

    fun invokeStatic(result: JvmCompilationResult, className: String, methodName: String, vararg args: Any?): Any? {
        val method = result.classLoader.loadClass(className).methods.first { method -> method.name == methodName }
        return method.invoke(null, *args)
    }

    fun errorsJson(result: Any?): String? {
        val validationResult = result as ValidationResult<*>
        if (validationResult !is InvalidResult<*>) {
            return null
        }
        return toJson(validationResult.errors)
    }

    private fun toJson(errors: ConstraintError): String {
        return jsonMapper.writeValueAsString(errors)
    }

}
