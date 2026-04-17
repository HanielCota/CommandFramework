import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    base
    alias(libs.plugins.shadow) apply false
    id("net.ltgt.errorprone") version "5.1.0" apply false
}

val sharedLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "io.github.hanielcota.commandframework"
version = (findProperty("version") as String?)
    ?.takeIf { it.isNotBlank() && it != "unspecified" }
    ?: "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        "errorprone"(sharedLibs.findLibrary("errorprone-core").get())
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // NOTE: Shadow 8.3.6 could not process Java 25 bytecode in this environment, so the build uses
        // Shadow 9.3.2 instead to preserve Java 25 output and required relocations.
        options.release.set(25)
        options.compilerArgs.add("-parameters")
        options.errorprone {
            disableWarningsInGeneratedCode.set(true)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    extensions.configure<CheckstyleExtension>("checkstyle") {
        toolVersion = sharedLibs.findVersion("checkstyle").get().requiredVersion
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = true
        isShowViolations = false
    }

    extensions.configure<PmdExtension>("pmd") {
        toolVersion = sharedLibs.findVersion("pmd").get().requiredVersion
        isConsoleOutput = false
        isIgnoreFailures = true
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }

    tasks.withType<Pmd>().configureEach {
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set("CommandFramework ${project.name}")
                    description.set(
                        "Annotation-based command framework for Paper and Velocity " +
                                "with a shared core, zero YAML registration, automatic parsing, " +
                                "permissions, cooldowns, confirmations, help, and tab-complete."
                    )
                    url.set("https://github.com/HanielCota/CommandFramework")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("HanielCota")
                            name.set("Haniel Cota")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/HanielCota/CommandFramework.git")
                        developerConnection.set("scm:git:ssh://github.com:HanielCota/CommandFramework.git")
                        url.set("https://github.com/HanielCota/CommandFramework")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/HanielCota/CommandFramework")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

project(":paper") {
    apply(plugin = "com.gradleup.shadow")

    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        relocate(
            "com.github.benmanes.caffeine",
            "io.github.hanielcota.commandframework.libs.caffeine"
        )
    }

    tasks.named("build") {
        dependsOn("shadowJar")
    }
}

project(":velocity") {
    apply(plugin = "com.gradleup.shadow")

    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        relocate(
            "com.github.benmanes.caffeine",
            "io.github.hanielcota.commandframework.libs.caffeine"
        )
    }

    tasks.named("build") {
        dependsOn("shadowJar")
    }
}
