rootProject.name = "labymod4-addon-template"

pluginManagement {
    val labyGradlePluginVersion = "0.8.1"
    val sdkVersion = providers.gradleProperty("de.ggbot.sdk-version").get()
    buildscript {
        repositories {
            maven("https://maven.laby.net/api/v1/maven/release/")
            maven("https://maven.neoforged.net/releases/")
            maven("https://maven.fabricmc.net/")
            gradlePluginPortal()
            mavenCentral()
        }

        dependencies {
            classpath("net.labymod.gradle", "common", labyGradlePluginVersion)
            classpath("de.ggbot","ggbot-sdk", sdkVersion)
        }
    }
}

plugins.apply("net.labymod.labygradle.settings")

include(":api")
include(":core")
