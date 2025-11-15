rootProject.name = "ggbot-addon"

pluginManagement {
    val labyGradlePluginVersion = "0.5.9"
    buildscript {
        repositories {
            maven("https://dist.labymod.net/api/v1/maven/release/")
            maven("https://maven.neoforged.net/releases/")
            maven("https://maven.fabricmc.net/")
            gradlePluginPortal()
            mavenCentral()
        }

        dependencies {
            classpath("net.labymod.gradle", "common", labyGradlePluginVersion)
            files("libs/openai-java-client-0.1.0.jar")
        }
    }
}

plugins.apply("net.labymod.labygradle.settings")

include(":api")
include(":core")
