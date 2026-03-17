import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
	idea
	alias(libs.plugins.loom)
	alias(libs.plugins.kotlin)
	alias(libs.plugins.ksp)
    `versioned-catalogues`
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
	toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
	withSourcesJar()
}

repositories {
	maven("https://maven.teamresourceful.com/repository/maven-public/")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
	maven("https://repo.hypixel.net/repository/Hypixel")
	maven("https://api.modrinth.com/maven")
	maven("https://maven.nucleoid.xyz/")
	mavenCentral()
    mavenLocal()
}

dependencies {
	minecraft(versionedCatalog["minecraft"])
	mappings(loom.officialMojangMappings())
    // loader + fabric kotlin
	modImplementation(libs.bundles.fabric)
    // fabricapi
    modImplementation(versionedCatalog["fabric"])

	api(libs.skyblockapi) {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}") }
	}
	include(libs.skyblockapi) {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}-remapped") }
	}
    api(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}") }
    }
    include(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}-remapped") }
    }

    // All needed for mlib rn ?????????
    includeImplementation(versionedCatalog["resourceful.lib"])
    includeImplementation(versionedCatalog["resourceful.config"])
    includeImplementation(versionedCatalog["placeholders"])
    includeImplementation(versionedCatalog["olympus"])

	modRuntimeOnly(libs.hypixel.modapi.fabric)

	implementation(libs.google.gson)

	compileOnly(libs.meowdding.ktmodules)
	ksp(libs.meowdding.ktmodules)

	modRuntimeOnly(libs.devauth)
}

fun DependencyHandler.includeImplementation(dep: Any) {
    include(dep)
    modImplementation(dep)
}

ksp {
	arg("meowdding.modules.project_name", "Pronouns")
	arg("meowdding.modules.package", "me.marie.pronouns.generated")
}

loom {
	runs {
		getByName("client") {
			ideConfigGenerated(true)
			property("devauth.configDir", rootProject.file(".devauth").absolutePath)
		}
	}
}

tasks {
    processResources {
        inputs.property("version", project.version)

        inputs.property("minecraft_version", versionedCatalog.versions["minecraft"])
        inputs.property("loader_version", libs.versions.loader.get())
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "minecraft_version" to versionedCatalog.versions["minecraft"],
                "loader_version" to libs.versions.loader.get(),
                "kotlin_loader_version" to libs.versions.fabrickotlin.get()
            )
        }
    }

    jar {
        from("LICENSE")
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=kotlin.uuid.ExperimentalUuidApi",
                "-Xcontext-parameters",
                "-Xcontext-sensitive-resolution",
                "-Xnested-type-aliases"
            )
        }
    }

    build {
        doLast {
            val sourceFile = rootProject.projectDir.resolve("versions/${project.name}/build/libs/pronoundb-$version.jar")
            val targetFile = rootProject.projectDir.resolve("build/libs/pronouns-$version-${stonecutter.current.version}.jar")
            targetFile.parentFile.mkdirs()
            targetFile.writeBytes(sourceFile.readBytes())
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}

