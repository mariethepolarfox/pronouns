import net.fabricmc.loom.task.ValidateAccessWidenerTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
	idea
    alias(libs.plugins.kotlin)
	alias(libs.plugins.loom)
	alias(libs.plugins.ksp)
    `versioned-catalogues`
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

    implementation(libs.fabricLoader)
    implementation(versionedCatalog["fabricApi"])
    implementation(libs.fabricKt)

    api(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}") }
    }
    include(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}") }
    }
    api(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}") }
    }
    include(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}") }
    }

    // All needed for mlib rn ?????????
    includeImplementation(versionedCatalog["resourceful.lib"])
    includeImplementation(versionedCatalog["resourceful.config"])
    includeImplementation(versionedCatalog["placeholders"])
    includeImplementation(versionedCatalog["olympus"])

	runtimeOnly(libs.hypixel.modapi.fabric)

	implementation(libs.google.gson)

	compileOnly(libs.meowdding.ktmodules)
	ksp(libs.meowdding.ktmodules)

    implementation(libs.devauth)
}

fun DependencyHandler.includeImplementation(dep: Any) {
    include(dep)
    implementation(dep)
}

ksp {
	arg("meowdding.modules.project_name", "Pronouns")
	arg("meowdding.modules.package", "me.marie.pronouns.generated")
}

var accessWidenerFile = rootProject.file("src/pronouns.accesswidener")
loom {
    accessWidenerPath = accessWidenerFile
    runConfigs["client"].apply {
        ideConfigGenerated(true)
        runDir = "../../run"
        vmArg("-Dfabric.modsFolder=" + '"' + rootProject.projectDir.resolve("run/${stonecutter.current.version.replace(".", "")}Mods").absolutePath + '"')
        property("devauth.configDir", rootProject.file(".devauth").absolutePath)
    }
}

tasks {
    processResources {
        val range = if (versionedCatalog.versions.has("minecraft.range")) {
            versionedCatalog.versions["minecraft.range"].toString()
        } else {
            val start = versionedCatalog.versions.getOrFallback("minecraft.start", "minecraft")
            val end = versionedCatalog.versions.getOrFallback("minecraft.end", "minecraft")
            ">=$start <=$end"
        }
        inputs.property("version", project.version)
        inputs.property("minecraft_version", range)
        inputs.property("loader_version", libs.versions.fabricLoader.get())

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "loader_version" to libs.versions.fabricLoader.get(),
                "minecraft_version" to range,
            )
        }

        with(copySpec {
            from(accessWidenerFile)
        })
    }

    jar {
        from("LICENSE")
        archiveFileName.set("Pronouns-$version-${stonecutter.current.version}.jar")
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }

    build {
        doLast {
            val sourceFile = rootProject.projectDir.resolve("versions/${project.name}/build/libs/Pronouns-$version-${stonecutter.current.version}.jar")
            val targetFile = rootProject.projectDir.resolve("build/libs/Pronouns-$version-${stonecutter.current.version}.jar")
            targetFile.parentFile.mkdirs()
            targetFile.writeBytes(sourceFile.readBytes())
        }
    }
}

tasks.withType<ValidateAccessWidenerTask> { enabled = false }

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_25
    sourceCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}
