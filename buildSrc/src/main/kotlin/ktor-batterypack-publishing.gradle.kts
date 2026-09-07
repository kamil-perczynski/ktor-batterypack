plugins {
    `maven-publish`
}

val ktorBatterypackPublishing =
    extensions.create<KtorBatterypackPublishingExtension>("ktorBatterypackPublishing").apply {
        component.convention("java")
        publication.convention("maven")
        artifactId.convention(null)
    }

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kamil-perczynski/ktor-batterypack")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>(ktorBatterypackPublishing.publication.get()) {
                from(components[ktorBatterypackPublishing.component.get()])
                ktorBatterypackPublishing.artifactId.orNull?.let { artifactId = it }
            }
        }
    }
}
