dependencies {
    api(project(":command-core"))
    api(project(":command-annotations"))
    compileOnly(libs.velocity.api)
    implementation(libs.caffeine)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.velocity.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}
