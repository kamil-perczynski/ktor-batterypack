package io.github.ktor_batterypack.validation.compile

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

class SourceGeneratingProcessorProvider(
    private val packageName: String,
    private val sources: Map<String, String>
) : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SourceGeneratingProcessor(environment)
    }

    private inner class SourceGeneratingProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

        private var generated = false

        override fun process(resolver: Resolver): List<KSAnnotated> {
            if (generated) {
                return emptyList()
            }
            for ((fileName, content) in sources) {
                environment.codeGenerator.createNewFile(Dependencies(false), packageName, fileName).use { output ->
                    output.write(content.trimIndent().toByteArray())
                }
            }
            generated = true
            return emptyList()
        }
    }
}
