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
            classpath("de.ggbot","ggbot-sdk","0.14.1")

        }
    }
}

plugins.apply("net.labymod.labygradle.settings")

include(":api")
include(":core")
