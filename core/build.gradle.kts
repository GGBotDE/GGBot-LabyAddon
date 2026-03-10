import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))
    addonMavenDependency("de.ggbot:ggbot-sdk:0.14.3")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}