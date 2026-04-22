dependencies {
    api(project(":command-core"))
    api(project(":command-annotations"))
    compileOnly(libs.velocity.api)
    implementation(libs.caffeine)
}
