dependencies {
    implementation(libs.bucket4j.core)
    implementation(libs.caffeine)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
