dependencies {
    api(project(":core"))
    // Adventure is compileOnly in core; the testkit uses Component in its public API,
    // so expose it transitively to consumers that pull in core-testkit.
    api(libs.adventure.api)
    // MessageService loads MiniMessage reflectively at runtime — required for build().
    implementation(libs.adventure.text.minimessage)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
