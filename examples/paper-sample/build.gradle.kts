plugins {
    `java-library`
    alias(libs.plugins.shadow)
    id("io.github.hanielcota.commandframework")
}

group = "com.example"
version = "1.0.0"
val commandFrameworkVersion = "0.2.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

commandframework {
    platform.set("paper")
    version.set(commandFrameworkVersion)
}

dependencies {
    compileOnly(libs.paper.api)
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // Relocate CommandFramework's shadowed deps if you plan to run alongside another
    // plugin that also uses Caffeine. The framework itself already relocates its copy.
}

tasks.build { dependsOn(tasks.shadowJar) }
