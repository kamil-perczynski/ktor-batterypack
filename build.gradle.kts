plugins {
    alias(libs.plugins.kotlin.jvm)
    `version-catalog`
    `maven-publish`
}

group = "io.github.kperczynski"
version = "0.0.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

catalog {
    versionCatalog {
        from(files("gradle/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
            artifactId = "ktor-batterypack-versions-catalog"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kamil-perczynski/ktor-batterypack")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
