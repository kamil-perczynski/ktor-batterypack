import org.gradle.api.provider.Property

interface KtorBatterypackPublishingExtension {
    val component: Property<String>
    val publication: Property<String>
    val artifactId: Property<String>
}
