import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileOutputStream
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "de.ggbot"
version = providers.environmentVariable("VERSION").getOrElse("1.0.0")

labyMod {
    defaultPackageName = "de.ggbot" //change this to your main package name (used by all modules)

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // When the property is set to true, you can log in with a Minecraft account
                    // devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "ggbot"
        displayName = "GGBot Addon"
        author = "GGBot.de"
        description = "Keine Ahnung"
        minecraftVersion = "*"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version
}

configurations.all {
    exclude(group = "com.google.code.gson", module = "gson")
}

tasks.register("removeGsonFromAddonJson") {
    description = "Removes com.google.code.gson dependencies from addon.json inside all built .jar files"
    group = "labymod"

    dependsOn(subprojects.mapNotNull { it.tasks.findByName("build") })

    doLast {
        val jarFiles = subprojects.flatMap { subproject ->
            val buildDir = subproject.layout.buildDirectory.asFile.get()
            buildDir.walkTopDown()
                    .filter { it.isFile && it.extension == "jar" && !it.name.contains("sources") && !it.name.contains("javadoc") }
                    .toList()
        }

        if (jarFiles.isEmpty()) {
            logger.warn("No .jar files found in subproject build directories.")
            return@doLast
        }

        jarFiles.forEach { jarFile ->
            val tempFile = File(jarFile.parent, jarFile.nameWithoutExtension + "_temp.jar")

            var addonJsonFound = false

            ZipFile(jarFile).use { zipIn ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zipOut ->
                    for (entry in zipIn.entries()) {
                        val inputStream = zipIn.getInputStream(entry)

                        if (entry.name == "addon.json") {
                            addonJsonFound = true

                            val json = JsonSlurper().parse(inputStream) as Map<*, *>
                            val mutableJson = json.toMutableMap()

                            @Suppress("UNCHECKED_CAST")
                            val deps = mutableJson["mavenDependencies"] as? List<Map<*, *>> ?: emptyList()
                            val filtered = deps.filter { dep -> dep["group"] != "com.google.code.gson" }
                            mutableJson["mavenDependencies"] = filtered

                            val newJsonBytes = JsonOutput.toJson(mutableJson).toByteArray(Charsets.UTF_8)
                            zipOut.putNextEntry(ZipEntry("addon.json"))
                            zipOut.write(newJsonBytes)
                            zipOut.closeEntry()

                            logger.lifecycle("Patched addon.json in ${jarFile.name}: removed ${deps.size - filtered.size} gson dep(s)")
                        } else {
                            zipOut.putNextEntry(ZipEntry(entry.name))
                            inputStream.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                    }
                }
            }

            if (addonJsonFound) {
                jarFile.delete()
                tempFile.renameTo(jarFile)
            } else {
                tempFile.delete()
                logger.warn("No addon.json found in ${jarFile.name}, skipping.")
            }
        }
    }
}

tasks.named("build") {
    finalizedBy("removeGsonFromAddonJson")
}