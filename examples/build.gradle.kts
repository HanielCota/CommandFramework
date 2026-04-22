plugins {
    java
}

dependencies {
    implementation(project(":command-paper"))
    implementation(project(":command-velocity"))
    compileOnly(libs.paper.api)
    compileOnly(libs.velocity.api)
}
