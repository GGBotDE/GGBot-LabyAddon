import net.labymod.labygradle.common.extension.LabyModAnnotationProcessorExtension.ReferenceType

dependencies {
    labyProcessor()
    api(project(":api"))
    api(files("libs/openai-java-client-0.1.0.jar"))
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("io.gsonfire:gson-fire:1.9.0")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:2.1.6")
    implementation("org.openapitools:jackson-databind-nullable:0.2.7")
    implementation(group = "org.apache.oltu.oauth2", name = "org.apache.oltu.oauth2.client", version = "1.0.2")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.18.0")
    implementation("jakarta.annotation:jakarta.annotation-api:1.3.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.mockito:mockito-core:3.12.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")

    // An example of how to add an external dependency that is used by the addon.
    // addonMavenDependency("org.jeasy:easy-random:5.0.0")
}

labyModAnnotationProcessor {
    referenceType = ReferenceType.DEFAULT
}