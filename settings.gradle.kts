pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") {
			name = "Fabric"
		}
		gradlePluginPortal()
		maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
	}
}