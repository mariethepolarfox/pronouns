
rootProject.name = "Pronouns"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.teamresourceful.com/repository/maven-public/")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.5"
}

val versions = listOf("26.1")

stonecutter {
    create(rootProject) {
        versions(versions)
        vcsVersion = versions.first()
    }
}


dependencyResolutionManagement {
    versionCatalogs {
        versions.forEach { version ->
            val versionName = version.replace('.', '_')
            create("libs${versionName.replace("_", "")}") {
                from(
                    files(
                        rootProject.projectDir.resolve("gradle/$versionName.versions.toml")
                    )
                )
            }
        }
    }
}
