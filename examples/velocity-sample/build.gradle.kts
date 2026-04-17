plugins {
    `java-library`
    alias(libs.plugins.shadow)
    id("io.github.hanielcota.commandframework")
}

group = "com.example"
version = "1.0.0"
val commandFrameworkVersion = "0.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

commandframework {
    platform.set("velocity")
    version.set(commandFrameworkVersion)
}

dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build { dependsOn(tasks.shadowJar) }
