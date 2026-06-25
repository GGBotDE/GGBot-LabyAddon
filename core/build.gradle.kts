import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))

    val sdkVersion = providers.gradleProperty("de.ggbot.sdk-version").get()
    addonMavenDependency("de.ggbot:ggbot-sdk:"+sdkVersion)
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}