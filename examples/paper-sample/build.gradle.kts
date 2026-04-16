plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.2"
}

group = "com.example"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    implementation("com.github.HanielCota.CommandFramework:paper:0.1.0")
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
