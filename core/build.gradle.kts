dependencies {
    api(project(":annotations"))
    api(libs.adventure.api)
    implementation(libs.adventure.text.minimessage)
    implementation(libs.caffeine)

    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testAnnotationProcessor(project(":processor"))
    testRuntimeOnly(libs.junit.platform.launcher)
}
