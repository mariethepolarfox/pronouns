import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.compile.JavaCompile

plugins {
	idea
	alias(libs.plugins.loom)
	alias(libs.plugins.kotlin)
	alias(libs.plugins.ksp)
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
	minecraft(libs.minecraft)
	mappings(loom.officialMojangMappings())
	modImplementation(libs.bundles.fabric)

	api(libs.skyblockapi) {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${libs.versions.minecraft.get()}") }
	}
	include(libs.skyblockapi) {
		capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${libs.versions.minecraft.get()}-remapped") }
	}
    api(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${libs.versions.minecraft.get()}") }
    }
    include(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${libs.versions.minecraft.get()}-remapped") }
    }

    // All needed for mlib rn ?????????
    includeImplementation(libs.resourceful.config)
    includeImplementation(libs.resourceful.lib)
    includeImplementation(libs.placeholders)
    includeImplementation(libs.olympus)

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
	arg("meowdding.modules.project_name", project.name)
	arg("meowdding.modules.package", "me.marie.pronouns.generated")
}

tasks.processResources {
	inputs.property("version", project.version)

	inputs.property("minecraft_version", libs.versions.minecraft.get())
	inputs.property("loader_version", libs.versions.loader.get())
	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(
			"version" to project.version,
			"minecraft_version" to libs.versions.minecraft.get(),
			"loader_version" to libs.versions.loader.get(),
			"kotlin_loader_version" to libs.versions.fabrickotlin.get()
		)
	}
}

loom {
	runs {
		getByName("client") {
			ideConfigGenerated(true)
			property("devauth.configDir", rootProject.file(".devauth").absolutePath)
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release.set(targetJavaVersion)
}


tasks.withType<KotlinCompile>().configureEach {
	compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
	compilerOptions {
		freeCompilerArgs.addAll(
			"-opt-in=kotlin.time.ExperimentalTime",
			"-opt-in=kotlin.uuid.ExperimentalUuidApi",
			"-Xcontext-parameters",
			"-Xcontext-sensitive-resolution",
			"-Xnested-type-aliases"
		)
	}
}

tasks.withType<Jar> {
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName}" }
	}
}
