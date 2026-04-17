val mockitoAgent: Configuration = configurations.create("mockitoAgent")

dependencies {
    api(project(":core"))
    compileOnly(libs.velocity.api)
    // NOTE: Keep this non-transitive so the shaded jar includes the MiniMessage parser without bundling
    // another copy of adventure-api classes that the platform already provides.
    runtimeOnly(libs.adventure.text.minimessage) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.velocity.api)

    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.test {
    jvmArgs("-javaagent:${mockitoAgent.asPath}")
}
