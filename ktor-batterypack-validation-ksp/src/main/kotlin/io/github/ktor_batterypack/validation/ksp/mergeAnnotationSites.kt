package io.github.ktor_batterypack.validation.ksp

fun mergeAnnotationSites(sites: List<List<CodegenAnnotation>>): List<CodegenAnnotation> {
    val merged = mutableListOf<CodegenAnnotation>()
    for (site in sites) {
        val mergedFqNames = merged.map { annotation -> annotation.fqName }.toSet()
        merged += site.filter { annotation -> annotation.fqName !in mergedFqNames }
    }
    return merged
}
