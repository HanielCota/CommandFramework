dependencies {
    api(project(":command-core"))
    api(project(":command-annotations"))
    compileOnly(libs.paper.api)
    implementation(libs.caffeine)
}
