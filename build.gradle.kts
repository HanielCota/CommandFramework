import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    base
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.spotless)
    alias(libs.plugins.jmh) apply false
}

group = "io.github.hanielcota.commandframework"
version = "0.1.0-SNAPSHOT"

val platformProjects = setOf("command-paper", "command-velocity")

sonar {
    properties {
        property("sonar.projectKey", "CommandFramework")
        property("sonar.projectName", "CommandFramework")
        property("sonar.projectVersion", project.version.toString())
        property("sonar.gradle.scanAll", "true")
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.exclusions", "**/build/**,**/.gradle/**")
    }
}

tasks.named("sonar") {
    dependsOn(tasks.named("build"))
}

spotless {
    format("gradle") {
        target("*.gradle.kts", "*/build.gradle.kts", "gradle/**/*.toml", "gradle.properties", ".editorconfig", ".gitignore")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")

    dependencies {
        val libsCatalog = rootProject.extensions
            .getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
            .named("libs")
        add("compileOnly", libsCatalog.findLibrary("jspecify").get())
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.withType<Javadoc>().configureEach {
        (options as org.gradle.external.javadoc.StandardJavadocDocletOptions)
            .addStringOption("Xdoclint:all,-missing", "-quiet")
    }

    val pitestConfiguration = configurations.maybeCreate("pitest")
    dependencies.add("pitest", "org.pitest:pitest-command-line:${rootProject.libs.versions.pitest.get()}")
    dependencies.add("pitest", "org.pitest:pitest-junit5-plugin:1.2.3")

    tasks.register<JavaExec>("pitest") {
        group = "verification"
        description = "Runs mutation testing with PIT"
        val sourceSets = project.extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
        classpath = pitestConfiguration + sourceSets["test"].runtimeClasspath + sourceSets["main"].output + sourceSets["test"].output
        mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")
        args = listOf(
            "--reportDir", layout.buildDirectory.dir("reports/pitest").get().asFile.absolutePath,
            "--targetClasses", "io.github.hanielcota.commandframework.${project.name.replace("-", ".").replace("command.", "")}.*",
            "--targetTests", "io.github.hanielcota.commandframework.${project.name.replace("-", ".").replace("command.", "")}.*",
            "--sourceDirs", sourceSets["main"].allSource.srcDirs.joinToString(","),
            "--outputFormats", "HTML,XML",
            "--timestampedReports", "false"
        )
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension>("spotless") {
        java {
            target("src/**/*.java")
            importOrder()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

configure(subprojects.filter { it.name in platformProjects }) {
    apply(plugin = "com.gradleup.shadow")

    tasks.named<Jar>("jar") {
        archiveClassifier.set("thin")
    }

    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        relocate("io.github.bucket4j", "io.github.hanielcota.commandframework.libs.bucket4j")
        relocate("com.github.benmanes.caffeine", "io.github.hanielcota.commandframework.libs.caffeine")
        relocate("com.google.errorprone", "io.github.hanielcota.commandframework.libs.errorprone")
        minimize()
        mergeServiceFiles()
    }

    tasks.named("assemble") {
        dependsOn(tasks.named("shadowJar"))
    }
}

configure(subprojects) {
    apply(plugin = "maven-publish")

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = rootProject.group.toString()
                artifactId = project.name
                version = rootProject.version.toString()
            }
        }
    }
}
