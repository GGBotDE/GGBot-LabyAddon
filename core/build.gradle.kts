import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))

    val sdkVersion = providers.gradleProperty("de.ggbot.sdk-version").get()
    addonMavenDependency("de.ggbot:ggbot-sdk:"+sdkVersion)

    compileOnly("maven.modrinth:replaymod:1.21.11-2.6.28")
}

repositories {
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}